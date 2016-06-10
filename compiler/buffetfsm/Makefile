#-------------------------------------------------------------------------------
# This Makefile is modeled after the one provided by 
# Eli Bendersky (eliben@gmail.com) in his LLVM/Clang code samples
# https://github.com/eliben/llvm-clang-samples
#
# Almost all explanatory comments below are courtesy of Eli,
# and worth a read for erudition's sake. Thanks, Eli.
#-------------------------------------------------------------------------------

# LLVM_SRC_PATH is the path to the root of the checked out source code. This
# directory should contain the configure script, the include/ and lib/
# directories of LLVM, Clang in tools/clang/, etc.
# Alternatively, if you're building vs. a binary download of LLVM, then
# LLVM_SRC_PATH can point to the main untarred directory.
LLVM_SRC_PATH := ./llvm

# LLVM_BUILD_PATH is the directory in which you built LLVM - where you ran
# configure or cmake.
# For linking vs. a binary build of LLVM, point to the main untarred directory.
# LLVM_BIN_PATH is the directory where binaries are placed by the LLVM build
# process. It should contain the tools like opt, llc and clang. The default
# reflects a debug build with autotools (configure & make).
LLVM_BUILD_PATH := ./llvm-build
LLVM_BIN_PATH := $(LLVM_BUILD_PATH)/Release+Asserts/bin

# CXX has to be a fairly modern C++ compiler that supports C++11. gcc 4.8 and
# higher or Clang 3.2 and higher are recommended. Best of all, if you build LLVM
# from sources, use the same compiler you built LLVM with.
CXX := g++
CXXFLAGS := -fno-strict-aliasing -fno-rtti -pedantic -pedantic-errors -Werror -Wall -Wextra -Wno-unused-parameter -Wpointer-arith -Wcast-qual -Wformat=2 -O2 -g
LDFLAGS := -Wl,-rpath=$$HOME/toolchains/lib

LLVM_CXXFLAGS := $(shell $(LLVM_BIN_PATH)/llvm-config --cxxflags)
LLVM_LDFLAGS := $(shell $(LLVM_BIN_PATH)/llvm-config --ldflags --libs --system-libs)

CLANG_INCLUDES := -I$(LLVM_SRC_PATH)/tools/clang/include -I$(LLVM_BUILD_PATH)/tools/clang/include

# List of Clang libraries to link. The proper -L will be provided by the
# call to llvm-config
# Note that I'm using -Wl,--{start|end}-group around the Clang libs; this is
# because there are circular dependencies that make the correct order difficult
# to specify and maintain. The linker group options make the linking somewhat
# slower, but IMHO they're still perfectly fine for tools that link with Clang.
CLANG_LIBS := \
	-Wl,--start-group \
	-lclangAST \
	-lclangAnalysis \
	-lclangBasic \
	-lclangDriver \
	-lclangEdit \
	-lclangFrontend \
	-lclangFrontendTool \
	-lclangLex \
	-lclangParse \
	-lclangSema \
	-lclangEdit \
	-lclangASTMatchers \
	-lclangRewriteCore \
	-lclangRewriteFrontend \
	-lclangStaticAnalyzerFrontend \
	-lclangStaticAnalyzerCheckers \
	-lclangStaticAnalyzerCore \
	-lclangSerialization \
	-lclangTooling \
	-lclangFormat \
	-Wl,--end-group

# header files to precompile
PCHDRS := buffetfsm_common.hpp

SRCFILES := $(wildcard *.cpp)
OBJFILES := ${SRCFILES:.cpp=.o}
GCHFILES := $(addsuffix .gch,$(PCHDRS))

# dependency files
DEPFILES := ${SRCFILES:.cpp=.o.d}
DEPFILES += ${GCHFILES:.gch=.gch.d}

# build invocations
BUILDCMDBASE = $(CXX) $(LLVM_CXXFLAGS) $(CXXFLAGS) 
LINKCMD = $(BUILDCMDBASE) $(OBJFILES) $(CLANG_LIBS) $(LDFLAGS) $(LLVM_LDFLAGS)
COMPILEBASE = $(BUILDCMDBASE) $(CLANG_INCLUDES) $<
COMPILECMD = $(COMPILEBASE) -c
DEPCMD = $(COMPILEBASE) -MM -MT "${@:.d=} $@" -MF $@ $<

SHOWINFO = echo "[$@] $(1)"

### TARGETS ###

.PHONY: all
all: buffetfsm

buffetfsm: $(GCHFILES) $(OBJFILES)
	@$(call SHOWINFO,Linking.)
	@$(LINKCMD) -o $@

%.o : %.cpp
	@$(call SHOWINFO,Compiling.)
	@$(COMPILECMD)

%.gch : %
	@$(call SHOWINFO,Compiling.)
	@$(COMPILECMD)

%.o.d : %.cpp
	@$(call SHOWINFO,Generating dependency database.)
	@$(DEPCMD)

%.gch.d : %
	@$(call SHOWINFO,Generating dependency database.)
	@$(DEPCMD)

.PHONY: clean
clean:
	@$(call SHOWINFO,Cleaning up.)
	@rm -rf buffetfsm $(GCHFILES) $(OBJFILES) $(DEPFILES) core

astdump:
	@echo clang++ -Xclang -ast-dump -std=c++11 -fsyntax-only

### DEP DATABASES ###

ifneq "$(MAKECMDGOALS)" "clean"
ifneq "$(MAKECMDGOALS)" "astdump"
-include $(DEPFILES)
endif
endif
