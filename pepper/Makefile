CXX = g++

# according to libsnark's documentation, we need to use the same conditional defines libsnark was compiled with, as found in
# thirdparty/libsnark/build/libsnark/CMakeFiles/snark.dir/flags.make
CXXFLAGS = -m64 -std=c++11 -DCURVE_BN128 -DBN_SUPPORT_SNARK -DBINARY_OUTPUT -DMONTGOMERY_OUTPUT -DNO_PROCPS -DUSE_ASM

AR	:= ar rcs

DEPSDIR := $(HOME)/pepper_deps

IFLAGS = -I./include -I. -I$(DEPSDIR)/include
# we use some of libsnark's internal dependencies which appear not to be
# installed properly by "make install" when installing libsnark, so we include
# libsnark and its dependencies directly from its build directory.
IFLAGS += -I../thirdparty/libsnark -I../thirdparty/libsnark/depends/libff -I../thirdparty/libsnark/depends/libfqfft


LDFLAGS = -L$(DEPSDIR)/lib
LDFLAGS += -L../thirdparty/libsnark/build/libsnark -L../thirdparty/libsnark/build/depends/libff/libff -L../thirdparty/libsnark/build/depends
LDFLAGS += -lsnark -lff -lzm -lgmp -lgmpxx -lpapi -lrt -lleveldb -lkyotocabinet -Wl,-rpath,$(DEPSDIR)/lib/

GENSRCDIR = gen
BINDIR = bin
OBJDIR = obj

DB_HASH_FUNC = ggh

#FLAGS FOR hashgetdb AND hashputdb: 
ifeq ($(DB_HASH_FUNC),null)
  DB_HASH_NUM_BITS = 256
else
   DB_HASH_NUM_BITS = 1216
endif


DB_NUM_ADDRESSES = 1024

# Threshold number of cells in RAM below which we want to use the naive solution to RAM
# The number below is calculated empirically for 32-bit cells in RAM and
# with current parameters of Ajtai's hash function
DB_THR_NUM_ADDRESSES_NAIVE = 32768

# Number of bits in a RAM cell (Note: if DB_NUM_ADDRESSES <
# DB_THR_NUM_ADDRESSES_NAIVE, this should be smaller than 32)
RAM_CELL_NUM_BITS = 32

FAST_RAM_ADDRESS_WIDTH = 32
FAST_RAM_WORD_WIDTH = 64


# DB flags
ifeq ($(DB_HASH_FUNC),null)
CXXFLAGS += -DDB_HASH_FUNC_IS_NULL
endif
CXXFLAGS += -DDB_NUM_ADDRESSES=$(DB_NUM_ADDRESSES) -DDB_HASH_NUM_BITS=$(DB_HASH_NUM_BITS)
CXXFLAGS += -DRAM_CELL_NUM_BITS=$(RAM_CELL_NUM_BITS)

CXXFLAGS += -DFAST_RAM_WORD_WIDTH=$(FAST_RAM_WORD_WIDTH)
CXXFLAGS += -DFAST_RAM_ADDRESS_WIDTH=$(FAST_RAM_ADDRESS_WIDTH)
CXXFLAGS += -DFOLDER_TMP=\"/tmp/$(USER)/tmp_state\"


ZCC_CARGS := --metrics -b 0 -w 10240 -t ZAATAR
ZCC_CARGS += -db-hash-func $(DB_HASH_FUNC)
ZCC_CARGS += -db-num-addresses $(DB_NUM_ADDRESSES)
ZCC_CARGS += -ram-cell-num-bits $(RAM_CELL_NUM_BITS)
ZCC_CARGS += -db-hash-num-bits $(DB_HASH_NUM_BITS)
ZCC_CARGS += -db-thr-num-addresses-naive $(DB_THR_NUM_ADDRESSES_NAIVE)
ZCC_CARGS += -fast-ram-address-width $(FAST_RAM_ADDRESS_WIDTH)
ZCC_CARGS += -fast-ram-word-width $(FAST_RAM_WORD_WIDTH)

include common/Makefrag
include storage/Makefrag
include include/Makefrag
include libv/Makefrag

COMMON_LIB_OBJFILES = \
	$(COMMON_OBJFILES) \
	$(LIBV_OBJFILES) \
	$(MERKLE_OBJFILES) \
	$(INCLUDE_OBJFILES) \
	$(PROVER_OBJFILES) \
	$(VERIFIER_OBJFILES)


pepper_verifier_%: pepper_verifier.cpp objs common_defs.h $(BINDIR)/%.params
	$(eval TARG := $(@:pepper_verifier_%=%))
	@echo ""
	@echo "========================="
	@echo "=== Compiling verifier =="
	@echo "========================="
	@echo ""
	$(CXX) $(CXXFLAGS) -DNAME=\"$(TARG)\" $(IFLAGS)  $<  -o $(BINDIR)/$@ $(COMMON_LIB_OBJFILES) $(LDFLAGS)

pepper_prover_%: pepper_prover.cpp objs common_defs.h $(BINDIR)/%.params
	$(eval TARG := $(@:pepper_prover_%=%))
	$(CXX) $(CXXFLAGS) -DNAME=\"$(TARG)\" $(IFLAGS)  $<  -o $(BINDIR)/$@ $(COMMON_LIB_OBJFILES) $(LDFLAGS)
	
objs: $(COMMON_LIB_OBJFILES)
		@echo + compiling common objs

VERDIR = verification_material
PDIR = proving_material
SHAREDIR = prover_verifier_shared


# Targets for invoking the C-to-constraints compiler.

# TODO: Find some makefile magic which eliminates the duplication for
# .c and .sfdl sources

$(BINDIR)/%.params : apps/%.c apps/*.h ../compiler/cstdinc/*
	@echo ""
	@echo "============================================"
	@echo "=== Compiling computation to constraints ==="
	@echo "============================================"
	@echo ""
	@echo + compile $< $(ZCC_CARGS)
	@mkdir -p $(BINDIR)
	@mkdir -p $(GENSRCDIR)
	@mkdir -p $(VERDIR)
	@mkdir -p $(PDIR)
	@mkdir -p $(SHAREDIR)
	@cd ../compiler/; \
	./zcc -f ../pepper/$< \
		      -d ../pepper \
			  -D $(DEPSDIR) \
		      $(ZCC_CARGS) \
		      -prefix $(patsubst apps/%.c,$(GENSRCDIR)/%,$<)


$(BINDIR)/%.params : apps/%.sfdl apps/*.h ../compiler/cstdinc/*
	@echo ""
	@echo "============================================"
	@echo "=== Compiling computation to constraints ==="
	@echo "============================================"
	@echo ""
	@echo + compile $< $(ZCC_CARGS)
	@mkdir -p $(BINDIR)
	@mkdir -p $(GENSRCDIR)
	@mkdir -p $(VERDIR)
	@mkdir -p $(PDIR)
	@mkdir -p $(SHAREDIR)
	@cd ../compiler/; \
	./zcc -f ../pepper/$< \
		      -d ../pepper \
			  -D $(DEPSDIR) \
		      $(ZCC_CARGS) \
		      -prefix $(patsubst apps/%.sfdl,$(GENSRCDIR)/%,$<)

$(OBJDIR)/%.o: %.cpp
	@echo + cpp $<
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) $(IFLAGS) -c $< -o $@ 

.PRECIOUS: $(BINDIR)/%.params

# make clean will not remove all of the obj files for the code in
# storage/, common/ and libv/, which make take a while to recompile.

clean-all: clean
	rm -rf $(OBJDIR)

clean:
	rm -rf $(GENSRCDIR)
	rm -rf $(BINDIR)
	rm -rf *.o pepper_prover pepper_verifier
	rm -rf $(VERDIR)
	rm -rf $(PDIR)
	rm -rf $(SHAREDIR)
	rm -f ../compiler/*circuit*
	rm -f ../compiler/*spec*
	rm -f ../compiler/*.c
	rm -f ../compiler/*.defines
	rm -f ../compiler/backend/*.pyc
	rm -f ../compiler/backend/tmptime
	rm -f ../compiler/tmptime
	cd ../compiler/frontend; make clean; 
	cd ../compiler/buffetfsm; make clean;

gadget%: ext_gadget_bridge.cpp
	$(CXX) $(CXXFLAGS) -UBINARY_OUTPUT -DNAME=\"$(TARG)\" $(IFLAGS)  $<  -o $(BINDIR)/$@ $(COMMON_LIB_OBJFILES) $(LDFLAGS)