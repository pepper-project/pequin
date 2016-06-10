//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//------------------------------------------------------------------------------
#ifndef INC_BREAKCONTINUEREWRITEVISITOR
#define INC_BREAKCONTINUEREWRITEVISITOR

#include "buffetfsm_common.hpp"

class AttributedStmtVisitor;
class BreakContinueRewriteVisitor : public RecursiveASTVisitor<BreakContinueRewriteVisitor> {
    public:
        BreakContinueRewriteVisitor(Rewriter &R, AttributedStmtVisitor &A);

        bool VisitContinueStmt(ContinueStmt *S);
        bool VisitBreakStmt(BreakStmt *S);
        bool TraverseAttributedStmt(AttributedStmt *A);
        bool TraverseCompoundStmt(CompoundStmt *S);

        bool needsRewrite(Stmt *S);
        bool doRewrite(Stmt *S, unsigned tState);

    private:
        Rewriter &TheRewriter;
        AttributedStmtVisitor &ParentVisitor;
        unsigned targetState = 0;
        bool foundStmt = false;
};

#endif
