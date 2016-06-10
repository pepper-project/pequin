//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//
// InLoopDeclVisitor is kicked off from each LoopFlattenAttr loop
// to be flattened, and extracts information about all variables
// declared inside (except when hidden by another LoopFlattenAttr)
//------------------------------------------------------------------------------
#include "InloopDeclVisitor.hpp"
#include "AttributedStmtVisitor.hpp"

InloopDeclVisitor::InloopDeclVisitor(AttributedStmtVisitor &P) : ParentVisitor(P) {}

// when inside a flattened loop, we pull out every declaration
// before flattening and push its scope outside the flattened loop
// so
// for () {
//  int A;
//  stuff;
// }
//
// becomes
//
// {
//  int A_uniquified;
//  for () {
//      /*  flattened stuff
//       *  with A rewritten to A_uniquified
//       */
//  }
// }
//
// outside of the flattened loop this is indistinguishable, but
// inside we need to make sure that visibility is maintained even
// as we add state machine if-guards

bool InloopDeclVisitor::findDecls(Stmt *S, std::vector<VarDecl*> *dVars) {
    declaredVars = dVars;
    return TraverseStmt(S);
}

bool InloopDeclVisitor::TraverseAttributedStmt(AttributedStmt *S) {
    // if the present AttributedStmt is a loop with LoopFlattenAttr,
    // this means that it is opaque from our flattening perspective.
    // Since we will not flatten this into the state machine (in other
    // words, the loop will remain its own independent loop), we do
    // not have to pull out declarations in order to maintain scoping
    // semantics

    if (nullptr != ParentVisitor.getLoopFlattenAttr(S)) {
        return true;
    } else {
        if (!WalkUpFromAttributedStmt(S)) { return false; }

        for (Stmt::child_range range = S->children(); range; ++range) {
            if (!TraverseStmt(*range)) { return false; }
        }

        return true;
    }
}

// every time we hit a VarDecl, push it onto the declaredVars stack
bool InloopDeclVisitor::VisitVarDecl(VarDecl *D) {
    declaredVars->push_back(D);
    return true;
}
