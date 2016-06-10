//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//------------------------------------------------------------------------------
#ifndef INC_INLOOPDECLVISITOR
#define INC_INLOOPDECLVISITOR

#include "buffetfsm_common.hpp"

class AttributedStmtVisitor;
class InloopDeclVisitor : public RecursiveASTVisitor<InloopDeclVisitor> {
    public:
        InloopDeclVisitor(AttributedStmtVisitor &P);
        bool VisitVarDecl(VarDecl *D);
        bool TraverseAttributedStmt(AttributedStmt *S);

        bool findDecls(Stmt *S, std::vector<VarDecl*> *dVars);

    private:
        AttributedStmtVisitor &ParentVisitor;
        std::vector<VarDecl*> *declaredVars = nullptr;
};

#endif
