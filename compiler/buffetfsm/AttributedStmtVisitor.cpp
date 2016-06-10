//------------------------------------------------------------------------------
// Loop flattening for \bPantry
//
// AttributedStmtVisitor walks the AST after we have gathered
// variable information, visiting all the AttributedStmt nodes
// and (in post-order) flattening loops as the programmer requests
//------------------------------------------------------------------------------
#include "AttributedStmtVisitor.hpp"
#include "InloopDeclVisitor.hpp"
#include "LFlatASTConsumer.hpp"
#include "VarDeclRewriteVisitor.hpp"

AttributedStmtVisitor::AttributedStmtVisitor(Rewriter &R, LFlatASTConsumer &C) : TheRewriter(R), TheConsumer(C), declRewriter(R, *this), bcRewriter(R, *this), idVisitor(*this) {}

// just pass this warning up the stack
void AttributedStmtVisitor::warn(std::string w) { TheConsumer.warn(w); }

// stateVar getter
std::string AttributedStmtVisitor::getStateVar() { return stateVar; }

// breakGuardVar getter
std::string AttributedStmtVisitor::getBreakGuardVar() { return breakGuardVar; }

// we override traverse rather than visit because we want to do
// post-order visiting. Traverse* calls WalkUp (which in turn calls Visit*)
// and then calls Traverse on each child. Once this is done, we take
// post-order actions.
bool AttributedStmtVisitor::TraverseAttributedStmt(AttributedStmt *S) {
    const LoopFlattenAttr *lfAttr = getLoopFlattenAttr(S,true);

    if (!WalkUpFromAttributedStmt(S)) {
        return false;
    }

    // If we are going to do rewriting below,
    // then we have to prepare uniqueNames here
    // so that any child loops to be flattened
    // can do our rewriting for us
    int numIters = lfAttr->getNumIters();
    std::vector<VarDecl*> inLoopDecls;
    std::stringstream topDecls;
    Stmt *loopStmt = S->getSubStmt();

    if ((lfAttr != nullptr) && (numIters != 0)) {
        // get declarations to be rewritten
        if (! idVisitor.findDecls(loopStmt, &inLoopDecls)) {
            return false;
        }

        // generate rewrite names and declarations for rewrite variables
        for (VarDecl *v : inLoopDecls) {
            std::string vType = v->getTypeSourceInfo()->getType().getAsString();
            std::string replName = createUniqueVar(v);

            topDecls << vType << ' ' << replName << ";\n";
        }
    }

    for (Stmt::child_range range = S->children(); range; ++range) {
        if (!TraverseStmt(*range)) {
            return false;
        }
    }

    // not a loop to be flattened
    if (lfAttr == nullptr) {
        return true;
    }

    // remove attribute
    // NOTE this will remove all attributes! (But probably there is no case where attributes matter in the output code...)
    SourceLocation attrStart = S->getLocStart();
    SourceLocation loopStart = loopStmt->getLocStart();
    SourceLocation loopPreStart = loopStart.getLocWithOffset(-1);
    TheRewriter.RemoveText(SourceRange(attrStart,loopPreStart));

    // if there are variable rewrites to do, do them
    // note that we do this even if numIters == 0 because
    // there could be rewrites from enclosing loopFlatten statements
    // that we have to process
    if (! uniqueNames.empty()) {
        declRewriter.TraverseStmt(loopStmt);
    }

    // if numIters == 0, the programmer has explicitly asked for
    // no unrolling at this level, so we do nothing. Note that
    // this is distinct from an un-annotated loop since in the
    // latter case it will be flattened into the containing
    // state machine.
    if (numIters == 0) {
        return true;
    }

    // find the end of the source in question
    const SourceManager &rwSM = TheRewriter.getSourceMgr();
    const LangOptions &rwLO = TheRewriter.getLangOpts();
    SourceLocation loopLocEnd = loopStmt->getLocEnd();
    SourceLocation loopEnd = Lexer::findLocationAfterToken(loopLocEnd, tok::semi, rwSM, rwLO, false);

    if (! loopEnd.isValid()) {
        loopEnd = Lexer::getLocForEndOfToken(loopLocEnd, 0, rwSM, rwLO);
    }
 
    // instantiate state, breakGuard, and count variables for this loop
    stateCnt = 0;
    stateVar = uniqueName("state");
    topDecls << "int " << stateVar << " = 0;\n";
    breakGuardVar = uniqueName("breakGuard");
    needBreakGuardVar = false; // don't insert breakGuardVar unless we actually need it (below)
    std::string countVar = uniqueName("count");
    topDecls << "int " << countVar << " = 0;\n";

    // flatten this loop recursively
    flattenLoop(loopStmt, true);

    // if we encountered any breaks, we will need the breakGuard as well
    if (needBreakGuardVar) {
        topDecls << "int " << breakGuardVar << " = 0;\n";
    }

    // now instantiate loop body
    topDecls << "while (" << countVar << " < " << numIters << ") {\n/*";

    // add top-level declarations for our uniquified inner declarations
    TheRewriter.InsertText(loopStart,"{ /****begin flattened loop****/\n",true,true);
    TheRewriter.InsertText(loopStart,topDecls.str(),true,true);

    // close the loop
    TheRewriter.InsertText(loopEnd,"\n" + countVar + "++;\n}\n}\n",true,true);

    // remove all of the declarations rewritten below this level from uniqueNames
    for (VarDecl *v : inLoopDecls) {
        uniqueNames.erase((void *) v);
    }

    return true;
}

// create a unique name given a base string
std::string AttributedStmtVisitor::uniqueName(std::string s) {
    s += "_0000000";
    int buff_i = 1;
    char buff[8];
    while (! TheConsumer.insertNewVar(s)) {
        snprintf(buff,8,"%7.7x",buff_i++);
        s.replace(s.length()-7,7,buff);
    }
    return s;
}

// create a new unique name for a VarDecl, and insert it into the variable name set and VarDecl->name mapping
std::string AttributedStmtVisitor::createUniqueVar(VarDecl *v) {
    std::string baseName = uniqueName(v->getDeclName().getAsString() + "_" + v->getTypeSourceInfo()->getType().getAsString());
    uniqueNames.insert(std::pair<void *,std::string>((void *) v, baseName));
    return baseName;
}

// given a Decl address, find out whether there is a corresponding unique name assigned
std::pair<bool,std::string> AttributedStmtVisitor::lookupUniqueVar(void *v) {
    std::map<void *,std::string>::iterator uVarIt = uniqueNames.find(v);
    if (uVarIt == uniqueNames.end()) {
        return std::pair<bool,std::string>(false,"");
    } else {
        return std::pair<bool,std::string>(true,(*uVarIt).second);
    }
}

// given an AttributedStmt, extract the LoopFlattenAttr associated, if any
const LoopFlattenAttr *AttributedStmtVisitor::getLoopFlattenAttr(AttributedStmt *S, bool w) {
    const LoopFlattenAttr *lfAttr = nullptr;

    for(llvm::ArrayRef<const Attr *>::iterator s = S->getAttrs().begin(), e = S->getAttrs().end(); s != e; s++) {
        if (isa<LoopFlattenAttr>(*s)) {
            if (w && (lfAttr != nullptr)) {
                warn("Found multiple pantry::flatten attributes on the same statement. All but the last are ignored.");
            }
            lfAttr = cast<LoopFlattenAttr>(*s);
        }
    }

    return lfAttr;
}

// is this statement a looping entity, i.e., one that
// will flatten into multiple states
bool AttributedStmtVisitor::isLoopingEntity(Stmt *S) {
    if (isa<WhileStmt>(S) ||
        isa<DoStmt>(S) ||
        isa<ForStmt>(S)) {
        return true;
    } else if (isa<AttributedStmt>(S)) {
        AttributedStmt *A = cast<AttributedStmt>(S);
        if ( isLoopingEntity(A->getSubStmt()) && (nullptr == getLoopFlattenAttr(A)) ) {
            return true;
        } else {
            return false;
        }
    } else {
        return false;
    }
}

// extract the loop body from a looping statement
Stmt *AttributedStmtVisitor::getLoopBody(Stmt *S) {
    if (isa<WhileStmt>(S)) {
        return cast<WhileStmt>(S)->getBody();
    } else if (isa<DoStmt>(S)) {
        return cast<DoStmt>(S)->getBody();
    } else if (isa<ForStmt>(S)) {
        return cast<ForStmt>(S)->getBody();
    } else if (isa<AttributedStmt>(S)) {
        return getLoopBody(cast<AttributedStmt>(S)->getSubStmt());
    } else {
        return nullptr;
    }
}

// count the total number of states we'll need to represent this statement
unsigned AttributedStmtVisitor::countStates(Stmt *S) {
    if (isLoopingEntity(S)) {
        return 2 + countStates(getLoopBody(S));
    } else if (isa<CompoundStmt>(S)) {
        bool inState = false;
        unsigned count = 0;
        for (Stmt::child_range range = S->children(); range; ++range) {
            bool isLoopOrCompound = isLoopingEntity(*range) || isa<CompoundStmt>(*range);
            if (isLoopOrCompound) {
                inState = false;
                count += countStates(*range);
            } else if (! inState) {
                inState = true;
                count++;
            }

            if (! isLoopOrCompound && bcRewriter.needsRewrite(*range)) {
                inState = false;
            }
        }
        return count;
    } else {
        return 1;
    }
}

// dispatch by loop type
void AttributedStmtVisitor::flattenLoop(Stmt *S, bool outerLoop) {
    if (isa<ForStmt>(S)) {
        flattenForLoop(cast<ForStmt>(S),outerLoop);
    } else if (isa<WhileStmt>(S)) {
        flattenWhileLoop(cast<WhileStmt>(S),outerLoop);
    } else if (isa<DoStmt>(S)) {
        flattenDoLoop(cast<DoStmt>(S),outerLoop);
    } else if (isa<AttributedStmt>(S)) {
        flattenLoop(cast<AttributedStmt>(S)->getSubStmt());
    } else {
        warn("Cannot flatten loop that is not do, while, or for. Output may be incorrect.");
    }
}

// do-loop flattening
void AttributedStmtVisitor::flattenDoLoop(DoStmt *S, bool outerLoop) {
    const SourceManager &rwSM = TheRewriter.getSourceMgr();
    const LangOptions &rwLO = TheRewriter.getLangOpts();
    Stmt *bodyStmt = S->getBody();
    unsigned startState = stateCnt;
    unsigned numStates = countStates(S);
    unsigned targetState = startState + numStates - 1;

    // comment out "do"
    if (! outerLoop) {
        TheRewriter.InsertText(S->getLocStart(),"/*",true,true);
    }
    TheRewriter.InsertText(Lexer::getLocForEndOfToken(S->getLocStart(), 0, rwSM, rwLO),"*/",true,true);
        
    // get the condition text
    std::string condStr = "(" + TheRewriter.getRewrittenText(SourceRange(S->getCond()->getLocStart(),S->getRParenLoc()));

    std::stringstream if1;
    if1 << "\nif (" << stateVar << " == " << startState << ") { " << stateVar << "++; }\n";
    // we always insert this (dummy) init state
    TheRewriter.InsertText(bodyStmt->getLocStart(),if1.str(),true,true);
    stateCnt++;

    // only used for a bare statement
    std::stringstream if2;
    if2 << "\nif (" << stateVar << " == " << startState + 1 << ") {\n";

    std::stringstream if3;
    if3 << "\nif (" << stateVar << " == " << targetState << ") {\n";
    if3 << "if (" << condStr << ") { " << stateVar << " = " << startState + 1 << "; }\n";
    if3 << "else { " << stateVar << "++; }\n}\n";

    // handle the body
    flattenBody(bodyStmt, targetState, if2.str(), if3.str());

    // comment out "while"
    TheRewriter.InsertText(S->getWhileLoc(),"/*",true,true);
    TheRewriter.InsertText(Lexer::findLocationAfterToken(S->getLocEnd(), tok::semi, rwSM, rwLO, false),"*/\n",true,true);
}

// for-loop flattening
void AttributedStmtVisitor::flattenForLoop(ForStmt *S, bool outerLoop) {
    Stmt *initStmt = S->getInit();
    Stmt *condStmt = S->getCond();
    Stmt *incStmt = S->getInc();
    Stmt *bodyStmt = S->getBody();
    unsigned startState = stateCnt;
    unsigned numStates = countStates(S);
    unsigned targetState = startState + numStates - 1;

    // comment out "for ..."
    if (! outerLoop) {
        TheRewriter.InsertText(S->getLocStart(),"/*",true,true);
    }
    TheRewriter.InsertText(bodyStmt->getLocStart(),"*/\n",true,true);

    // get the text for initializer, conditional, and increment
    const SourceManager &rwSM = TheRewriter.getSourceMgr();
    const LangOptions &rwLO = TheRewriter.getLangOpts();
    SourceLocation initEnd = Lexer::getLocForEndOfToken(initStmt->getLocEnd(), 1, rwSM, rwLO);
    SourceLocation condEnd = Lexer::getLocForEndOfToken(condStmt->getLocEnd(), 1, rwSM, rwLO);
    SourceLocation incEnd = Lexer::getLocForEndOfToken(incStmt->getLocEnd(), 1, rwSM, rwLO);
    std::string initStr = TheRewriter.getRewrittenText(SourceRange(initStmt->getLocStart(),initEnd));
    std::string condStr = TheRewriter.getRewrittenText(SourceRange(condStmt->getLocStart(),condEnd));
    std::string incStr = TheRewriter.getRewrittenText(SourceRange(incStmt->getLocStart(),incEnd));

    // create the states surrounding the loop
    std::stringstream if1;
    if1 << "\nif (" << stateVar << " == " << startState << ") {\n" << initStr << ";\n";
    if1 << "if (" << condStr << ") { " << stateVar << "++; }\n";
    if1 << "else { " << stateVar << " = " << startState + numStates << "; }\n}\n";
    // insert the init state
    TheRewriter.InsertText(bodyStmt->getLocStart(),if1.str(),true,true);
    stateCnt++;

    // only used for a bare statement
    std::stringstream if2;
    if2 << "\nif (" << stateVar << " == " << startState + 1 << ") {\n";

    std::stringstream if3;
    if3 << "\nif (" << stateVar << " == " << targetState << ") {\n" << incStr << ";\n";
    if3 << "if (" << condStr << ") { " << stateVar << " = " << startState + 1 << "; }\n";
    if3 << "else { " << stateVar << "++; }\n}\n";

    // handle the body
    flattenBody(bodyStmt, targetState, if2.str(), if3.str());
}

// while-loop flattening
void AttributedStmtVisitor::flattenWhileLoop(WhileStmt *S, bool outerLoop) {
    Stmt *condStmt = S->getCond();
    Stmt *bodyStmt = S->getBody();
    unsigned startState = stateCnt;
    unsigned numStates = countStates(S);
    unsigned targetState = startState + numStates - 1;

    // comment out "while ..."
    if (! outerLoop) {
        TheRewriter.InsertText(S->getLocStart(),"/*", true, true);
    }
    TheRewriter.InsertText(bodyStmt->getLocStart(),"*/\n", true, true);

    // get text for conditional
    const SourceManager &rwSM = TheRewriter.getSourceMgr();
    const LangOptions &rwLO = TheRewriter.getLangOpts();
    SourceLocation condEnd = Lexer::getLocForEndOfToken(condStmt->getLocEnd(), 1, rwSM, rwLO);
    std::string condStr = TheRewriter.getRewrittenText(SourceRange(condStmt->getLocStart(),condEnd));

    // create the states surrounding the loop
    std::stringstream if1;
    if1 << "\nif (" << stateVar << " == " << startState << ") {\n";
    if1 << "if (" << condStr << ") { " << stateVar << "++; }\n";
    if1 << "else { " << stateVar << " = " << startState + numStates << "; }\n}\n";
    // insert the init state
    TheRewriter.InsertText(bodyStmt->getLocStart(),if1.str(),true,true);
    stateCnt++;

    // only used for a bare statement
    std::stringstream if2;
    if2 << "\nif (" << stateVar << " == " << startState + 1 << ") {\n";

    std::stringstream if3;
    if3 << "\nif (" << stateVar << " == " << targetState << ") {\n";
    if3 << "if (" << condStr << ") { " << stateVar << " = " << startState + 1 << "; }\n";
    if3 << "else { " << stateVar << "++; }\n}\n";

    // handle the body
    flattenBody(bodyStmt, targetState, if2.str(), if3.str());
}

// flatten a loop body independent of loop type
void AttributedStmtVisitor::flattenBody (Stmt *bodyStmt, unsigned targetState, std::string &&st2, std::string &&st3) {
    const SourceManager &rwSM = TheRewriter.getSourceMgr();
    const LangOptions &rwLO = TheRewriter.getLangOpts();

    // find the sorcelocation after the end of the body
    SourceLocation semiEnd = Lexer::findLocationAfterToken(bodyStmt->getLocEnd(), tok::semi, rwSM, rwLO, false);
    if (! semiEnd.isValid()) {
        semiEnd = Lexer::getLocForEndOfToken(bodyStmt->getLocEnd(), 0, rwSM, rwLO);
    }

    std::string stateEnd = "\n" + stateVar + "++;\n}\n";
    std::string clearBreakEnd = "\n" + breakGuardVar + " = 0;\n" + stateEnd;

    // body of statement is one of three things: compound statement, or simple statement
    // but simple statement could in principle be another loop
    if (isa<CompoundStmt>(bodyStmt)) {
        flattenCompoundStmt(cast<CompoundStmt>(bodyStmt), targetState);

        assert ( (stateCnt == targetState) &&
                 "Number of states disagrees after flattening CompoundStmt." );
    } else if (isLoopingEntity(bodyStmt)) {
        flattenLoop(bodyStmt);

        assert ( (stateCnt == targetState) &&
                 "Number of states disagrees after flattening loop nested inside do-loop.");
    } else {
        TheRewriter.InsertText(bodyStmt->getLocStart(),st2,true,true);

        // rewrite break or continue statement, if necessary
        if (bcRewriter.doRewrite(bodyStmt, targetState)) {
            //warn("break and continue semantics may not be preserved. Check output carefully!");
            TheRewriter.InsertText(semiEnd, clearBreakEnd, true, true);
            needBreakGuardVar = true;
        } else {
            TheRewriter.InsertText(semiEnd, stateEnd, true, true);
        }

        stateCnt += 1;
    }

    TheRewriter.InsertText(semiEnd, st3, true, true);
    stateCnt += 1;
}


// flatten a compound statement (that is, { ... })
void AttributedStmtVisitor::flattenCompoundStmt(CompoundStmt *S, unsigned targetState) {
    TheRewriter.ReplaceText(S->getLBracLoc(),1,"");
    TheRewriter.ReplaceText(S->getRBracLoc(),1,"");

    bool inState = false;
    std::string stateEnd = "\n" + stateVar + "++;\n}\n";
    std::string clearBreakEnd = "\n" + breakGuardVar + " = 0;\n" + stateEnd;

    for (Stmt::child_range range = S->children(); range; ++range) {
        bool isLoop = isLoopingEntity(*range);
        bool isLoopOrCompound = isLoop || isa<CompoundStmt>(*range);
        if (isLoopOrCompound) {
            // make sure the present state, if any, is closed
            if (inState) {
                TheRewriter.InsertText((*range)->getLocStart(),stateEnd,true,true);
                inState = false;
            }

            // now handle
            if (isLoop) {
                flattenLoop(*range);
            } else {
                flattenCompoundStmt(cast<CompoundStmt>(*range), targetState);
            }
        } else if (!inState) {
            // otherwise, make sure we're in a state
            std::stringstream if1;
            if1 << "\nif (" << stateVar << " == " << stateCnt << ") {\n";
            TheRewriter.InsertText((*range)->getLocStart(),if1.str(),false,true);
            stateCnt++;
            inState = true;
        }

        // now, if the present state is a break or continue, we rewrite it, and
        // then immediately end this state
        if (! isLoopOrCompound && bcRewriter.doRewrite(*range, targetState)) {
            //warn("break and continue semantics may not be preserved. Check output carefully!");
            const SourceManager &rwSM = TheRewriter.getSourceMgr();
            const LangOptions &rwLO = TheRewriter.getLangOpts();

            // find the sorcelocation after the end
            SourceLocation stmtEnd = (*range)->getLocEnd();
            SourceLocation semiEnd = Lexer::findLocationAfterToken(stmtEnd, tok::semi, rwSM, rwLO, false);
            if (! semiEnd.isValid()) {
                semiEnd = Lexer::getLocForEndOfToken(stmtEnd, 0, rwSM, rwLO);
            }
            TheRewriter.InsertText(semiEnd,clearBreakEnd,true,true);
            needBreakGuardVar = true;
            inState = false;
        }
    }

    if (inState) {
        TheRewriter.InsertText(S->getRBracLoc(),stateEnd,true,true);
    }
}
