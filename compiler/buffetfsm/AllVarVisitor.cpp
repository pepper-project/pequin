//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//
// AllVarVisitor goes through the AST and pulls out all VarDecls
// so that we can be sure to construct unique variable names
//------------------------------------------------------------------------------
#include "AllVarVisitor.hpp"
#include "LFlatASTConsumer.hpp"

AllVarVisitor::AllVarVisitor(LFlatASTConsumer &l) : lC(l) {}

bool AllVarVisitor::VisitVarDecl(VarDecl *D) {
    lC.insertNewVar(D->getDeclName().getAsString());
    return true;
}
