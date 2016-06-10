//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//------------------------------------------------------------------------------
#ifndef INC_ALLVARVISITOR
#define INC_ALLVARVISITOR

#include "buffetfsm_common.hpp"

class LFlatASTConsumer;
class AllVarVisitor : public RecursiveASTVisitor<AllVarVisitor> {
    public:
        AllVarVisitor(LFlatASTConsumer &l);
        bool VisitVarDecl(VarDecl *D);

    private:
        LFlatASTConsumer &lC;
};

#endif
