package ccomp.parser_hw;

import java.util.ArrayList;

import ccomp.parser_hw.HLL_ASTNodes.TypeSpecification;

import SFE.Compiler.Operators.BitwiseAndOperator;
import SFE.Compiler.Operators.BitwiseOrOperator;
import SFE.Compiler.Operators.BitwiseShiftOperator;
import SFE.Compiler.Operators.BitwiseXOROperator;
import SFE.Compiler.Operators.DivisionOperator;
import SFE.Compiler.Operators.MinusOperator;
import SFE.Compiler.Operators.Operator;
import SFE.Compiler.Operators.PlusOperator;
import SFE.Compiler.Operators.TimesOperator;

/**
 * Some classes useful for building an abstract syntax tree for code written in a high level language,
 * specifically large subsets of Java or C. May also work for a large subset of fortran.
 */
public class HLL_ASTNodes {
  public static class Identifier implements Expression {
    public String identifier;
    //Can declare a multidimensional array of the same type as thie identifier
    public ArrayList<Expression> arraySizes = new ArrayList();
    public ArrayList<VariableDeclaration> args = null;
    public TypeSpecification pointer = null;

    public Identifier(String identifier) {
      this.identifier = identifier;
    }

    public void setArgumentDeclarations(ArrayList<VariableDeclaration> args) {
      //Remove void
      if (args.size() == 1){
        VariableDeclaration vd = args.get(0);
        if (vd.type.spec.equals("void")){
          this.args = new ArrayList();
          return;
        }
      }
      this.args = args;
    }

    /**
     * Set arguments, none of which are typed so we use default
     */
    public void setDefaultArgumentDeclarations(ArrayList<Identifier> untypedArgs) {
      args = new ArrayList();
      for(Identifier q : untypedArgs){
        args.add(new VariableDeclaration(new TypeSpecification(TypeSpecification.UNKNOWN), q));
      }
    }
    
    public void setPointer(TypeSpecification pointer){
      this.pointer = pointer;
    }

    public void addArrayDeclaration(Expression c) {
      arraySizes.add(c);
    }

    public String toString() {
      return identifier+" "+arraySizes+" args: "+args+" pointer: "+pointer;
    }
  }
  //Make an id which is guaranteed to be distinct between temporary values in the program
  //so, Foo() + Foo() will introduce temporary store locations for the return value of 
  //Foo based on parse location (static scope) and not dynamic scope (which introduces waste)
  private static int PARSE_UNIQUIFIER = 0;
  
  public static class FunctionCall implements Expression {
    public Identifier funcName;
    public ArrayList<Expression> args;
    public int parse_uid;
    public FunctionCall(Identifier funcName, ArrayList<Expression> args) {
      super();
      this.funcName = funcName;
      this.args = args;
      this.parse_uid = PARSE_UNIQUIFIER++;
    }
  }
  public static class Constant implements Expression {
    /**
     * For now, just store constant as string. Translate when working with the nodes.
     */
    public Object value;
    /**
     * For convenience, pass along a hint string from the lexer.
     * 
     * ??? - unknown
     * int hex - hexadecimal integer constant
     * int octal - octal integer constant
     * int decimal - decimal integer constant
     * fp - floating point
     * string - string literal
     */
    public String constType;
    public SFE.Compiler.Expression const_expr; //Used by CCompiler to keep around parsed information
    public Constant(Object d, String constType) {
      value = d;
      this.constType = constType;
    }
  }
  //Any nonfunctional declaration. I.e. either a typedef, struct def, or a variable declaration (or multiple of these) 
  public static interface Declaration{
    
  }
  public static class TypeSpecification implements Declaration{
    public static final String UNKNOWN = ":unknown:";
    
    public String spec;

    public TypeSpecification(String spec) {
      this.spec = spec;
    }

    public void appendTypeSpecification(TypeSpecification other) {
      if (spec == null || spec.equals("")){
        spec = other.spec;
        return;
      }
      spec += " "+other.spec;
    }
    
    public boolean isUnknown(){
      return spec.contains(UNKNOWN);
    }
    public boolean isSubType(TypeSpecification other){
      if (other.isUnknown()){
        throw new RuntimeException("I don't know how to tell if something is a subtype of unknown");
      }
      if (isUnknown()){
        return true;
      }
      //Weak for now.
      return other.spec.equals(spec);
    }

    public String toString() {
      return spec;
    }
  }
  public static class TypeDefSpecification extends TypeSpecification {
    public TypeSpecification type;

    public TypeDefSpecification(String spec) {
      super(spec);
    }

    public void appendTypeSpecification(TypeSpecification other) {
      if (type != null){
        throw new RuntimeException("I don't know how to have typedef accept more than one type: "+type+" and "+other);
      }
      type = other;
    }

    public String toString() {
      return spec+" "+type;
    }
  }
  public static class StructTypeSpecification extends TypeSpecification {
    String structType;
    String structName; 
    ArrayList<VariableDeclaration> components;
    public StructTypeSpecification(String structType, String structName,
        ArrayList<VariableDeclaration> components) {
      super(structType + " "+structName);
      this.structType = structType;
      this.structName = structName;
      this.components = components;
    }
  }
  public static class MultiVariableDeclaration extends ArrayList<VariableDeclaration> implements Declaration{
    
  }
  public static class VariableDeclaration {
    public TypeSpecification type;
    public Identifier name;
    public Expression initializer;
    public VariableDeclaration(Identifier name) {
      this.name = name;
    }
    public VariableDeclaration(TypeSpecification type){
      this.type = type;
      if (type.spec.equals("void")){
        //A.OK.
      } else {
        throw new RuntimeException("I don't know how to treat type "+type+" as a variable declaration");
      }
    }
    public VariableDeclaration(TypeSpecification type, Identifier name) {
      this.type = type;
      this.name = name;
    }
    public VariableDeclaration(TypeSpecification type, AssignmentStatement init){
      this.type = type;
      this.name = init.LHS;
      this.initializer = init.RHS; //may be null
    }
    public void setType(TypeSpecification type) {
      this.type = type;
    }
    public void setInitializer(Expression e){
      initializer = e;
    }
  }
  public static interface Statement {

  }
  
  public static class CompoundStatement implements Statement {
    public ArrayList<Object> statements_declarations = new ArrayList();
    public CompoundStatement(ArrayList<? extends Object> statements_declarations) {
      this.statements_declarations.addAll(statements_declarations);
    }
    public CompoundStatement(ArrayList<Declaration> declarations, Statement statement) {
      statements_declarations.addAll(declarations);
      statements_declarations.add(statement);
    }
    public CompoundStatement() {
      //Empty statement
    }
  }
  public static class ContinueStatement implements Statement {
  }
  public static class BreakStatement implements Statement {
  }
  public static class ReturnStatement implements Statement {
    public Expression toRet;
    public ReturnStatement(Expression toReturn) {
      this.toRet = toReturn;
    }
  }
  /**
   * As an expression, return LHS after assigning to it.
   */
  public static class AssignmentStatement implements Statement, Expression {
    public Identifier LHS;
    public Expression LHS_lookup;
    public Operator binaryOperator; //for +=, this is PlusOperator, for normal assignment, this is null.
    public Expression RHS;
    //Note: rHS can only be null if this is the first assignment to the variable LHS.
    public AssignmentStatement(Identifier lHS, Expression rHS) {
      super();
      LHS = lHS;
      RHS = rHS;
    }
    public AssignmentStatement(Expression LHS_lookup, String op, Expression value){
      this.LHS_lookup = LHS_lookup;
      this.RHS = value;
      if (op.equals("=")){
        binaryOperator = null;
      } else if (op.length()==2 && op.charAt(1) == '='){
        switch(op.charAt(0)){
        case '+':
          binaryOperator = new PlusOperator();
          break;
        case '-':
          binaryOperator = new MinusOperator();
          break;
        case '*':
          binaryOperator = new TimesOperator();
          break;
        case '/':
          binaryOperator = new DivisionOperator(DivisionOperator.QUOTIENT);
          break;
        case '%':
          binaryOperator = new DivisionOperator(DivisionOperator.REMAINDER);
          break;
        case '|':
          binaryOperator = new BitwiseOrOperator();
          break;
        case '&':
          binaryOperator = new BitwiseAndOperator();
          break;
        case '^':
          binaryOperator = new BitwiseXOROperator();
          break;
        }
        if (binaryOperator==null){
          throw new RuntimeException("Assignment operator "+binaryOperator+" not yet supported");
        }
      } else if (op.equals("<<=")){
        binaryOperator = new BitwiseShiftOperator(BitwiseShiftOperator.LEFT_SHIFT);
      } else if (op.equals(">>=")){
        //Note: C does not have a special arithmetic shift operator. We assume all right shifts are arithmetic.
        //(Arithmetic shift == logical shift unless signed case, and the left argument is negative.)
        binaryOperator = new BitwiseShiftOperator(BitwiseShiftOperator.RIGHT_SHIFT);
      } else {
        throw new RuntimeException("Assignment operator "+op+" not yet supported");
      }
    }
  }
  
  /**
   * Generalizes fors, whiles, dos, etc.
   */
  public static class LoopStatement implements Statement {
    public Statement pre;
    public Expression enter;
    public Statement block;
    /**
     * Pre: Statement to run before loop
     * Enter: Expression with boolean value determining whether to enter loop. Reevaluated each time loop repeats.
     * Block: Code in the block of the loop
     * Advance: (Second part of block): Code to always run after running block. Seperate from block for expressive purposes only.
     */
    public LoopStatement(Statement pre, Expression enter, Statement block, Statement advance) {
      this.pre = pre;
      this.enter = enter;
      ArrayList<Statement> block2 = new ArrayList();
      if (block != null){
        block2.add(block);
      }
      if (advance != null){
        block2.add(advance);
      }
      this.block = new CompoundStatement(block2);
    }
  }
  public static interface Expression extends Statement {

  }
  public static class ConditionalExpression implements Expression {
    public Expression cond;
    public Expression the;
    public Expression els;
    public int parse_uid;

    public ConditionalExpression(Expression cond, Expression the, Expression els){
      this.cond = cond;
      if (els == null || the == null){
        throw new RuntimeException("Cannot construct conditional expression without both then and else expressions");
      }
      this.the = the;
      this.els = els;
      this.parse_uid = PARSE_UNIQUIFIER++;
    }
  }
  public static class IfStatement implements Expression, Statement {
    public Expression cond;
    public Statement the;
    public Statement els;
    /**
     * Used when the C compiler generates code
     */
    public SFE.Compiler.Expression cond_override;
    public int parse_uid;

    public IfStatement(Expression cond, Statement the, Statement els){
      this.cond = cond;
      if (els == null || the == null){
        throw new RuntimeException("Cannot construct if statements without both then and else blocks");
      }
      this.the = the;
      this.els = els;
      this.parse_uid = PARSE_UNIQUIFIER++;
    }    
    public IfStatement(SFE.Compiler.Expression cond, Statement the, Statement els){
      this.cond = null;
      this.cond_override = cond;
      if (els == null || the == null){
        throw new RuntimeException("Cannot construct if statements without both then and else blocks");
      }
      this.the = the;
      this.els = els;
    }    
  }
  public static class CastExpression implements Expression {
    public TypeSpecification type;
    public Expression toCast;
    public CastExpression(TypeSpecification type, Expression toCast) {
      this.type = type;
      this.toCast = toCast;
    }
  }
  public static class MultiExpression extends ArrayList<Expression> implements Expression {
    public MultiExpression(){
      super();
    }
    public MultiExpression(ArrayList<Expression> toAdd){
      super();
      addAll(toAdd);
    }
  }
  public static class BinaryOpExpression implements Expression {
    public Expression left;
    public SFE.Compiler.Operators.Operator operator;
    public Expression right;
    public BinaryOpExpression(Expression left, Operator operator, Expression right) {
      super();
      this.left = left;
      this.operator = operator;
      this.right = right;
    }
  }
  public static class UnaryOpExpression implements Expression {
    public SFE.Compiler.Operators.Operator operator;
    public Expression middle;
    public UnaryOpExpression(Operator operator, Expression middle) {
      super();
      this.operator = operator;
      this.middle = middle;
    }
  }
  public static class FunctionDefinition {
    public Identifier functionNameAndArgs;
    public TypeSpecification returnType;
    public Statement block;
    public FunctionDefinition(TypeSpecification returnType, Identifier functionNameAndArgs, Statement block) {
      super();
      this.functionNameAndArgs = functionNameAndArgs;
      this.returnType = returnType;
      this.block = block;
    }
    public FunctionDefinition(Identifier functionNameAndArgs, Statement block) {
      this(new TypeSpecification("int"), functionNameAndArgs, block);
      //System.err.println("Warning - function with default int return type"); This is pretty standard, and
      //is the case for function which don't return anything (i.e. saves writing "void")
    }
  }
  public static class Program extends ArrayList<Object>{
    public void addProgram(Program other){
      addAll(other);
    }
  }
}
