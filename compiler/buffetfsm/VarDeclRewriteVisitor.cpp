//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//
// VarDeclRewriteVisitor is tasked with finding and rewriting
// DeclRefExprs and DeclStmts pertaining to VarDecls that are
// being rewritten underneath an AttributedStmt
//------------------------------------------------------------------------------
#include "VarDeclRewriteVisitor.hpp"
#include "AttributedStmtVisitor.hpp"

VarDeclRewriteVisitor::VarDeclRewriteVisitor(Rewriter &R, AttributedStmtVisitor &A) : TheRewriter(R), ParentVisitor(A) {}

bool VarDeclRewriteVisitor::VisitDeclRefExpr(DeclRefExpr *D) {
    std::pair<bool,std::string> uVarPair = ParentVisitor.lookupUniqueVar((void *) D->getDecl());
    if (! uVarPair.first) {
        return true;
    }

    // this is a decl we must rewrite
    TheRewriter.ReplaceText(SourceRange(D->getLocStart(), D->getLocEnd()), StringRef(uVarPair.second));

    return true;
}

// DeclStmt has one or more VarDecl
// we rewrite them, generally by just commenting them out
// but in some cases by adding a new assignment in their place
// e.g.,
//    int i;
//      -> commented out
//    int j = 10;
//      -> becomes "j_rewrite_name = 10;"
bool VarDeclRewriteVisitor::VisitDeclStmt(DeclStmt *D) {

    const SourceManager &rwSM = TheRewriter.getSourceMgr();
    const LangOptions &rwLO = TheRewriter.getLangOpts();
    SourceLocation declStart = D->getLocStart();
    SourceLocation declEnd = Lexer::getLocForEndOfToken(D->getLocEnd(), 0, rwSM, rwLO);

    for (DeclStmt::decl_iterator s = D->decl_begin(), e = D->decl_end(); s!=e; s++) {
        std::pair<bool,std::string> uVarPair = getVarPair(s);
        if (!uVarPair.first) { continue; }

        VarDecl *vDecl = cast<VarDecl>(*s);
        if (vDecl->hasInit()) {
            // walk the initializer, rewriting
            Expr *vDeclInit = vDecl->getInit();
            if (! TraverseStmt(vDeclInit)) {
                return false;
            }

            TheRewriter.InsertText(vDeclInit->getLocStart(), "\n" + uVarPair.second + " = ",true,true);
            TheRewriter.InsertText(Lexer::getLocForEndOfToken(vDecl->getLocEnd(), 0, rwSM, rwLO),";\n//",true,true);
        }
    }

    // comment out the original declaration
    TheRewriter.InsertText(declStart,"//",true,true);
    //TheRewriter.InsertText(D->getLocEnd(),"/*",true,true);
    TheRewriter.InsertText(declEnd,"\n",true,true);

    return true;
}

bool VarDeclRewriteVisitor::TraverseDeclStmt(DeclStmt *D) {
    // do not visit children, Visit does that for us
    return WalkUpFromDeclStmt(D);
}

// do not traverse LoopFlattenAttr'd statements ---
// these will have already been rewritten (since
// we are called from AttributedStmtVisitor, which does
// post-order rewriting)
bool VarDeclRewriteVisitor::TraverseAttributedStmt(AttributedStmt *A) {
    if (nullptr != ParentVisitor.getLoopFlattenAttr(A)) {
        return true;
    } else {
        if (! WalkUpFromAttributedStmt(A)) {
            return false;
        }

        for (Stmt::child_range range = A->children(); range; ++range) {
            if (! TraverseStmt(*range)) {
                return false;
            }
        }

        return true;
    }
}

bool VarDeclRewriteVisitor::TraverseForStmt(ForStmt *F) {
    Stmt *S = F->getInit();
    if (isa<DeclStmt>(S)) {
        DeclStmt *D = cast<DeclStmt>(S);
        for (DeclStmt::decl_iterator s = D->decl_begin(), e = D->decl_end(); s!=e; s++) {
            std::pair<bool,std::string> uVarPair = getVarPair(s);
            if (!uVarPair.first) { continue; }

            VarDecl *vDecl = cast<VarDecl>(*s);
            TheRewriter.ReplaceText(SourceRange(vDecl->getLocStart(),vDecl->getInit()->getLocStart().getLocWithOffset(-1)), StringRef(uVarPair.second + " = "));
            if (vDecl->hasInit()) {
                if (! TraverseStmt(vDecl->getInit())) {
                    return false;
                }
            }
        }
    } else {
        if (! TraverseStmt(S)) {
            return false;
        }
    }

    return TraverseStmt(F->getCond()) && TraverseStmt(F->getInc()) && TraverseStmt(F->getBody());
}

std::pair<bool,std::string> VarDeclRewriteVisitor::getVarPair(DeclStmt::decl_iterator s) {
    std::pair<bool,std::string> uVarPair = ParentVisitor.lookupUniqueVar((void*) *s);
    const SourceManager &rwSM = TheRewriter.getSourceMgr();
    if (! uVarPair.first) {
        // this should not happen, but just warn for now
        ParentVisitor.warn("Did not find decl at " + (*s)->getLocStart().printToString(rwSM) + " among uniqueVars. Output code may be incorrect.");
    }
    if (! isa<VarDecl>(*s)) {
        // this should not happen, but just warn for now
        ParentVisitor.warn("Found non-VarDecl child of DeclStmt at " + (*s)->getLocStart().printToString(rwSM) + ". Output code may be incorrect.");
        uVarPair.first = false;
    }
    return uVarPair;
}
