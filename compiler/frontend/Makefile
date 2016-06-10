BINDIR :=bin
CASSANDRA :=thirdparty/cassandra/
CQLTRANS := $(BINDIR)/cql/CQLToKVStore.class
BEAVERCC :=thirdparty/Beaver/lib/beaver-cc.jar
JFLEX :=thirdparty/jflex/lib/JFlex.jar
GENSRC :=gensrc
TEMP :=temp
JAVA :=java
JAVAC :=javac
SRCDIR :=src

SCANNERS := $(GENSRC)/ccomp/parser/CScanner.java
PARSERS := $(GENSRC)/ccomp/parser/CParser.java 
MAIN := zcc/ZCC.java

all: $(SCANNERS) $(PARSERS) geninterp
cql: $(CQLTRANS)

#CQL translator
$(CQLTRANS) : $(SRCDIR)/cql/*.java
	@mkdir -p $(BINDIR)/cql
	$(JAVAC) -cp $(CASSANDRA)/build/*:$(CASSANDRA)/lib/* -d $(BINDIR)/cql $(SRCDIR)/cql/*.java

#Beaver
$(GENSRC)/ccomp/parser/%Parser.java : $(SRCDIR)/%.grammar 
	$(JAVA) -jar $(BEAVERCC) \
	  -d $(GENSRC) \
	  $<

#Jflex
$(GENSRC)/ccomp/parser/%Scanner.java : $(SRCDIR)/%.flex 
	$(JAVA) -jar $(JFLEX) \
	  -d $(GENSRC)/ccomp/parser \
	  $<

#javac
geninterp:
	ant compile
#	@mkdir -p $(TEMP)
#	cp -r $(GENSRC)/* $(TEMP)/
#	cp -r $(SRCDIR)/* $(TEMP)/
#	@mkdir -p $(BINDIR)
#	$(JAVAC) -sourcepath $(TEMP) \
#		 -d $(BINDIR) \
#	  	 $(TEMP)/$(MAIN)

# For deleting the build
clean:	
	ant clean
	rm -rf $(GENSRC)
#	rm -rf $(BINDIR)
#	rm -rf $(TEMP)
