//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//
// NOTE: this code must be linked against a patched Clang
// with support for the [[pantry::flatten(x)]] annotation.
// Nested loops so annotated will be transformed into flat
// state machines.
//
// Based on "Clang rewriter sample", rewritersample.cpp,
// from Eli Bendersky (eliben@gmail.com)
// https://github.com/eliben/llvm-clang-samples
//------------------------------------------------------------------------------
#include "buffetfsm.hpp"

int main(int argc, char *argv[]) {
    if (argc < 2) {
        llvm::errs() << "Usage: " << argv[0] << " [preprocessor options] <filename>\n";
        llvm::errs() << "<filename> can be - for stdin.\n";
        return 1;
    }

    // CompilerInstance will hold the instance of the Clang compiler for us,
    // managing the various objects needed to run the compiler.
    CompilerInstance TheCompInst;
    TheCompInst.createDiagnostics();

    CompilerInvocation *inv = new CompilerInvocation;
    CompilerInvocation::CreateFromArgs(*inv, argv+1, argv+argc, TheCompInst.getDiagnostics());
    TheCompInst.setInvocation(inv);

    LangOptions &lo = TheCompInst.getLangOpts();
    lo.CPlusPlus = 1;
    lo.CPlusPlus11 = 1;

    // Initialize target info with the default triple for our platform.
    TargetOptions *TO = new TargetOptions();
    TO->Triple = llvm::sys::getDefaultTargetTriple();
    TargetInfo *TI =
        TargetInfo::CreateTargetInfo(TheCompInst.getDiagnostics(), TO);
    TheCompInst.setTarget(TI);

    TheCompInst.createFileManager();
    FileManager &FileMgr = TheCompInst.getFileManager();
    TheCompInst.createSourceManager(FileMgr);
    SourceManager &SourceMgr = TheCompInst.getSourceManager();
    TheCompInst.createPreprocessor(TU_Module);
    TheCompInst.createASTContext();

    // A Rewriter helps us manage the code rewriting task.
    Rewriter TheRewriter;
    TheRewriter.setSourceMgr(SourceMgr, lo);

    // Set the main file handled by the source manager to the input file.
    const FileEntry *FileIn;
    std::unique_ptr<llvm::MemoryBuffer> inBuf;
    if (! strncmp("-",argv[argc-1], 2)) {
        if (llvm::error_code ec = llvm::MemoryBuffer::getFileOrSTDIN("-", inBuf)) {
            llvm::errs() << ec.message() << "\n";
            return 1;
        }

        FileIn = FileMgr.getVirtualFile("<stdin>", inBuf->getBufferSize(), 0);
        SourceMgr.overrideFileContents(FileIn, inBuf.get(), true);
    } else {
        FileIn = FileMgr.getFile(argv[argc - 1]);
        if (nullptr == FileIn || ! FileIn->isValid()) {
            llvm::errs() << "Error: could not open input file.\n";
            return 1;
        }
    }

    SourceMgr.setMainFileID(SourceMgr.createFileID(FileIn, SourceLocation(), SrcMgr::C_User));
    DiagnosticConsumer &TheDiagConsumer = TheCompInst.getDiagnosticClient();
    TheDiagConsumer.BeginSourceFile(lo, &TheCompInst.getPreprocessor());

    // Create an AST consumer instance which is going to get called by
    // ParseAST.
    LFlatASTConsumer TheConsumer(TheRewriter);

    // Parse the file to AST, registering our consumer as the AST consumer.
    ParseAST(TheCompInst.getPreprocessor(), &TheConsumer,
            TheCompInst.getASTContext());
    TheDiagConsumer.EndSourceFile();

    // At this point the rewriter's buffer should be full with the rewritten
    // file contents.
    const RewriteBuffer *RewriteBuf =
        TheRewriter.getRewriteBufferFor(SourceMgr.getMainFileID());

    unsigned numErrors = TheDiagConsumer.getNumErrors();
    unsigned numWarnings = TheDiagConsumer.getNumWarnings() + TheConsumer.numWarnings;
    int retVal = 0;

    if (numErrors == 0) {
        llvm::MemoryBuffer *mBuf;
        if (nullptr != RewriteBuf) {
            //llvm::outs() << std::string(RewriteBuf->begin(), RewriteBuf->end());
            mBuf = llvm::MemoryBuffer::getMemBufferCopy(StringRef(std::string(RewriteBuf->begin(), RewriteBuf->end())), StringRef("rewrite"));
        } else {
            // no rewriting done, but no errors, so just put out the original file
            llvm::errs() << "** Warning: no flattening necessary for input file. **\n";
            numWarnings++;
            if (! strncmp("-",argv[argc-1], 2)) {
                mBuf = inBuf.release();
            } else {
                mBuf = FileMgr.getBufferForFile(FileIn);
            }
        }

        // now we have the input file in a memory buffer, put it through clang's formatting facility
        // set up the file manager with this new pseudo-file
        const FileEntry *ReformatFile = FileMgr.getVirtualFile("rewrite", mBuf->getBufferSize(), 0);
        SourceMgr.overrideFileContents(ReformatFile, mBuf, false);
        FileID reformatFileID = SourceMgr.createFileID(ReformatFile, SourceLocation(), SrcMgr::C_User);

        // use LLVM formatting style
        format::FormatStyle fStyle = format::getLLVMStyle();

        // lex
        Lexer Lex(reformatFileID, mBuf, SourceMgr, lo);

        // the source range is just the whole file
        std::vector<CharSourceRange> ranges;
        ranges.push_back(CharSourceRange::getCharRange(SourceMgr.getLocForStartOfFile(reformatFileID),SourceMgr.getLocForEndOfFile(reformatFileID)));

        // apply the formatting
        tooling::Replacements Replaces = reformat(fStyle, Lex, SourceMgr, ranges);
        tooling::applyAllReplacements(Replaces, TheRewriter);

        // print the result
        TheRewriter.getEditBuffer(reformatFileID).write(llvm::outs());
    } else {
        retVal = 1;
    }

    if (numErrors + numWarnings) {
        llvm::errs() << "** Flattening completed with " << numErrors << " errors and " << numWarnings << " warnings. **\n";
    }

    return retVal;
}
