//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//------------------------------------------------------------------------------
#ifndef INC_VARDECLREWRITEVISITOR
#define INC_VARDECLREWRITEVISITOR

#include "buffetfsm_common.hpp"

class AttributedStmtVisitor;
class VarDeclRewriteVisitor : public RecursiveASTVisitor<VarDeclRewriteVisitor> {
    public:
        VarDeclRewriteVisitor(Rewriter &R, AttributedStmtVisitor &A);
        bool VisitDeclRefExpr(DeclRefExpr *D);
        bool VisitDeclStmt(DeclStmt *D);
        bool TraverseDeclStmt(DeclStmt *D);
        bool TraverseForStmt(ForStmt *F);
        bool TraverseAttributedStmt(AttributedStmt *A);
        std::pair<bool,std::string> getVarPair(DeclStmt::decl_iterator s);

    private:
        Rewriter &TheRewriter;
        AttributedStmtVisitor &ParentVisitor;
};

#endif
