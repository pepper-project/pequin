//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//
// The semantics of "break" and "continue" change drastically when we
// flatten loops. To fix this, we must rewrite breaks and continues in
// terms of the flat state machine.
//------------------------------------------------------------------------------
#include "BreakContinueRewriteVisitor.hpp"
#include "AttributedStmtVisitor.hpp"

BreakContinueRewriteVisitor::BreakContinueRewriteVisitor(Rewriter &R, AttributedStmtVisitor &A) : TheRewriter(R), ParentVisitor(A) {}

bool BreakContinueRewriteVisitor::needsRewrite(Stmt *S) {
    // with tState = 0, no rewrite will happen, it will only report
    // existence of ContinueStmt and BreakStmt below Stmt
    return doRewrite(S, 0);
}

bool BreakContinueRewriteVisitor::doRewrite(Stmt *S, unsigned tState) {
    targetState = tState;
    foundStmt = false;

    TraverseStmt(S);

    return foundStmt;
}

bool BreakContinueRewriteVisitor::VisitContinueStmt(ContinueStmt *S) {
    // OK, we found a continue Statement
    foundStmt = true;

    // do a rewrite if targetState is nonzero
    // a "continue" is equivalent to jumping to the continue clause of the present loop
    // in our state machine, the continue clause is targetState, but at the end of
    // every state we increment stateVar, so here we make stateVar equal targetState - 1
    // so that after increment it will correctly reflect targetState
    if (targetState) {
        const SourceManager &rwSM = TheRewriter.getSourceMgr();
        const LangOptions &rwLO = TheRewriter.getLangOpts();

        // comment out the "continue"
        TheRewriter.InsertText(S->getLocStart(),"/*",true,true);
        // find the sorcelocation after the end
        SourceLocation continueEnd = S->getLocEnd();
        SourceLocation semiEnd = Lexer::findLocationAfterToken(continueEnd, tok::semi, rwSM, rwLO, false);
        if (! semiEnd.isValid()) {
            semiEnd = Lexer::getLocForEndOfToken(continueEnd, 0, rwSM, rwLO);
        }
        TheRewriter.InsertText(semiEnd,"*/\n",true,true);

        // insert the state
        std::stringstream cRep;
        cRep << ParentVisitor.getStateVar() << " = " << targetState - 1 << ";\n";
        cRep << ParentVisitor.getBreakGuardVar() << " = 1;\n";
        TheRewriter.InsertText(semiEnd,cRep.str(),true,true);
    }

    return true;
}

bool BreakContinueRewriteVisitor::VisitBreakStmt(BreakStmt *S) {
    // found a break statement
    foundStmt = true;

    // do a rewrite if targetState is nonzero
    // a "break" is equivalent to jumping past the continue clause of the present loop
    // in our state machine, the continue clause is targetState. As above, the increment
    // at the end of every state pushes us up by 1, so here we just set it equal to target
    // and after this state finishes it will equal targetState + 1
    if (targetState) {
        const SourceManager &rwSM = TheRewriter.getSourceMgr();
        const LangOptions &rwLO = TheRewriter.getLangOpts();

        // comment out the "continue"
        TheRewriter.InsertText(S->getLocStart(),"/*",true,true);
        // find the sorcelocation after the end
        SourceLocation continueEnd = S->getLocEnd();
        SourceLocation semiEnd = Lexer::findLocationAfterToken(continueEnd, tok::semi, rwSM, rwLO, false);
        if (! semiEnd.isValid()) {
            semiEnd = Lexer::getLocForEndOfToken(continueEnd, 0, rwSM, rwLO);
        }
        TheRewriter.InsertText(semiEnd,"*/\n",true,true);

        // insert the state
        std::stringstream cRep;
        cRep << ParentVisitor.getStateVar() << " = " << targetState << ";\n";
        cRep << ParentVisitor.getBreakGuardVar() << " = 1;\n";
        TheRewriter.InsertText(semiEnd,cRep.str(),true,true);
    }

    return true;
}

// we don't push through further LoopFlattenAttrs when flattening Break and Continue;
// they are flattened with respect to their own loops
bool BreakContinueRewriteVisitor::TraverseAttributedStmt(AttributedStmt *A) {
    if (! WalkUpFromAttributedStmt(A)) {
        return false;
    }

    const LoopFlattenAttr *lfAttr = ParentVisitor.getLoopFlattenAttr(A);
    if (nullptr != lfAttr) {
        // flattened loop below; stop recursing here
        return true;
    }

    for (Stmt::child_range range = A->children(); range; ++range) {
        if (! TraverseStmt(*range)) {
            return false;
        }
    }

    return true;
}

bool BreakContinueRewriteVisitor::TraverseCompoundStmt (CompoundStmt *S){
    if (! WalkUpFromCompoundStmt(S)) {
        return false;
    }

    std::string bGuard = "\n if (! " + ParentVisitor.getBreakGuardVar() + ") {\n";

    // we want to know if a particular child has a continue or break
    // thus we overwrite foundStmt and allow recursion to continue
    // we restore the state when we return (except that of course
    // if we find a break or continue below, foundStmt = true)
    bool foundStmtSave = foundStmt;
    bool needGuard = false;
    for (Stmt::child_range range = S->children(); range; ++range) {
        if (needGuard && targetState) {
            // we need to add a break-guard around the remainder of this compound statement
            TheRewriter.InsertText((*range)->getLocStart(),bGuard,true,true);
            TheRewriter.InsertText(S->getLocEnd(),"\n}\n",true,true);
            needGuard = false;
        }

        foundStmt = false;
        if (! TraverseStmt(*range)) {
            return false;
        }

        // if we found something below, make sure that it gets passed on
        foundStmtSave |= foundStmt;
        // now, if we found something here, the remainder of this compoundstmt (if any) requires a guard
        // it will be added next time around the loop
        needGuard = foundStmt;
    }

    // restore value of foundStmt and return
    foundStmt = foundStmtSave;
    return true;
}
