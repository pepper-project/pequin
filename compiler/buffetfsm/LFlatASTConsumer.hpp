//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//------------------------------------------------------------------------------
#ifndef INC_LFLATASTCONSUMER
#define INC_LFLATASTCONSUMER

#include "buffetfsm_common.hpp"
#include "AttributedStmtVisitor.hpp"
#include "AllVarVisitor.hpp"

class LFlatASTConsumer : public ASTConsumer {
    public:
        LFlatASTConsumer(Rewriter &R);
        virtual void HandleTranslationUnit(ASTContext &C);

    // variable registry functions
        bool insertNewVar(const std::string &s);

    // track warnings generated
        unsigned numWarnings = 0;
        void warn(std::string w);

    private:
        AllVarVisitor avVisitor;
        AttributedStmtVisitor Visitor;
        std::set<std::string> varNames;
};

#endif
