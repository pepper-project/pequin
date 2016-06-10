//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//------------------------------------------------------------------------------
#ifndef INC_ATTRIBUTEDSTMTVISITOR
#define INC_ATTRIBUTEDSTMTVISITOR

#include "buffetfsm_common.hpp"
#include "VarDeclRewriteVisitor.hpp"
#include "BreakContinueRewriteVisitor.hpp"
#include "InloopDeclVisitor.hpp"

class LFlatASTConsumer;
class AttributedStmtVisitor : public RecursiveASTVisitor<AttributedStmtVisitor> {
    public:
        AttributedStmtVisitor(Rewriter &R, LFlatASTConsumer &C);
        bool TraverseAttributedStmt(AttributedStmt *S);

    // unique variable support for rewriting
        std::string uniqueName(std::string s);
        std::string createUniqueVar(VarDecl *v);
        std::pair<bool,std::string> lookupUniqueVar(void *v);

        const LoopFlattenAttr *getLoopFlattenAttr(AttributedStmt *S, bool w=false);
        void warn(std::string w);

        std::string getStateVar();
        std::string getBreakGuardVar();

    private:
        Rewriter &TheRewriter;
        LFlatASTConsumer &TheConsumer;
        VarDeclRewriteVisitor declRewriter;
        BreakContinueRewriteVisitor bcRewriter;
        InloopDeclVisitor idVisitor;
        std::map<void *,std::string> uniqueNames;
        std::string stateVar = "";
        std::string breakGuardVar = "";
        bool needBreakGuardVar = false;
        unsigned stateCnt = 0;

    // methods used for flattening
        bool isLoopingEntity(Stmt *S);
        Stmt *getLoopBody(Stmt *S);
        unsigned countStates(Stmt *S);
        void flattenLoop(Stmt *S, bool outerLoop = false);
        void flattenForLoop(ForStmt *S, bool outerLoop);
        void flattenWhileLoop(WhileStmt *S, bool outerLoop);
        void flattenDoLoop(DoStmt *S, bool outerLoop);
        void flattenCompoundStmt(CompoundStmt *S, unsigned targetState);
        void flattenBody(Stmt *bodyStmt, unsigned targetState, std::string &&st2, std::string &&st3);
};

#endif
