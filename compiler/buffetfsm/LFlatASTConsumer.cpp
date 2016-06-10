//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//
// LFlatASTConsumer is the ASTConsumer, which is to say, the
// dispatcher for all of the AST walkers. It is executed after
// Clang parses the source file.
//------------------------------------------------------------------------------
#include "LFlatASTConsumer.hpp"
#include "AttributedStmtVisitor.hpp"
#include "AllVarVisitor.hpp"

LFlatASTConsumer::LFlatASTConsumer(Rewriter &R) : avVisitor(*this), Visitor(R, *this) {}

// insert new variable name into the set
bool LFlatASTConsumer::insertNewVar(const std::string &s) {
    return varNames.insert(s).second;
}

void LFlatASTConsumer::HandleTranslationUnit (ASTContext &C) {
    Decl *tuDecl = C.getTranslationUnitDecl();
    // first, walk the AST and visit all the VarDecl nodes,
    // adding them to the list of variable declarations so
    // that we can be sure to construct unique variable names
    avVisitor.TraverseDecl(tuDecl);
    Visitor.TraverseDecl(tuDecl);
}

void LFlatASTConsumer::warn (std::string w) {
    numWarnings++;
    llvm::errs() << "** Warning: " << w << " **\n";
}
