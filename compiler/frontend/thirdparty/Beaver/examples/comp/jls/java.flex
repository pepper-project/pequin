/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Copyright (C) 1998-2001  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * This program is free software; you can redistribute it and/or modify    *
 * it under the terms of the GNU General Public License. See the file      *
 * COPYRIGHT for more information.                                         *
 *                                                                         *
 * This program is distributed in the hope that it will be useful,         *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License along *
 * with this program; if not, write to the Free Software Foundation, Inc., *
 * 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA                 *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

/* Java 1.2 language lexer specification */

/* Use together with unicode.flex for Unicode preprocesssing */
/* and java12.grammar a Java 1.2 parser                      */

/* Note that this lexer specification is not tuned for speed.
   It is in fact quite slow on integer and floating point literals.
   For a production quality application (e.g. a Java compiler)
   this could be optimized */

/* 2003-12: Modified to work with Beaver parser generator */

package comp.jls;

import beaver.Symbol;
import beaver.Scanner;

import comp.jls.JavaParser.Terminals;

%%

%class JavaScanner
%extends Scanner
%unicode
%function nextToken
%type Symbol
%yylexthrow Scanner.Exception
%eofval{
	return token(Terminals.EOF, "end-of-file");
%eofval}
%line
%column

%{
  StringBuffer string = new StringBuffer(128);

  private Symbol token(short id)
  {
	return new Symbol(id, yyline + 1, yycolumn + 1, yylength(), yytext());
  }

  private Symbol token(short id, Object value)
  {
	return new Symbol(id, yyline + 1, yycolumn + 1, yylength(), value);
  }

%}


/* main character classes */
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]

WhiteSpace = {LineTerminator} | [ \t\f]

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment} |
          {DocumentationComment}

TraditionalComment = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}?
DocumentationComment = "/*" "*"+ [^/*] ~"*/"

/* identifiers */
Identifier = [:jletter:][:jletterdigit:]*

/* integer literals */
DecIntegerLiteral = 0 | [1-9][0-9]*
DecLongLiteral    = {DecIntegerLiteral} [lL]

HexIntegerLiteral = 0 [xX] 0* {HexDigit} {1,8}
HexLongLiteral    = 0 [xX] 0* {HexDigit} {1,16} [lL]
HexDigit          = [0-9a-fA-F]

OctIntegerLiteral = 0+ [1-3]? {OctDigit} {1,15}
OctLongLiteral    = 0+ 1? {OctDigit} {1,21} [lL]
OctDigit          = [0-7]

/* floating point literals */
FloatLiteral  = ({FLit1}|{FLit2}|{FLit3}) {Exponent}? [fF]
DoubleLiteral = ({FLit1}|{FLit2}|{FLit3}) {Exponent}?

FLit1    = [0-9]+ \. [0-9]*
FLit2    = \. [0-9]+
FLit3    = [0-9]+
Exponent = [eE] [+-]? [0-9]+

/* string and character literals */
StringCharacter = [^\r\n\"\\]
SingleCharacter = [^\r\n\'\\]

%state STRING, CHARLITERAL

%%

<YYINITIAL> {

  /* keywords */
  "abstract"                     { return token(Terminals.ABSTRACT); }
  "boolean"                      { return token(Terminals.BOOLEAN); }
  "break"                        { return token(Terminals.BREAK); }
  "byte"                         { return token(Terminals.BYTE); }
  "case"                         { return token(Terminals.CASE); }
  "catch"                        { return token(Terminals.CATCH); }
  "char"                         { return token(Terminals.CHAR); }
  "class"                        { return token(Terminals.CLASS); }
//  "const"                        { return token(Terminals.CONST); }
  "continue"                     { return token(Terminals.CONTINUE); }
  "do"                           { return token(Terminals.DO); }
  "double"                       { return token(Terminals.DOUBLE); }
  "else"                         { return token(Terminals.ELSE); }
  "extends"                      { return token(Terminals.EXTENDS); }
  "final"                        { return token(Terminals.FINAL); }
  "finally"                      { return token(Terminals.FINALLY); }
  "float"                        { return token(Terminals.FLOAT); }
  "for"                          { return token(Terminals.FOR); }
  "default"                      { return token(Terminals.DEFAULT); }
  "implements"                   { return token(Terminals.IMPLEMENTS); }
  "import"                       { return token(Terminals.IMPORT); }
  "instanceof"                   { return token(Terminals.INSTANCEOF); }
  "int"                          { return token(Terminals.INT); }
  "interface"                    { return token(Terminals.INTERFACE); }
  "long"                         { return token(Terminals.LONG); }
  "native"                       { return token(Terminals.NATIVE); }
  "new"                          { return token(Terminals.NEW); }
//  "goto"                         { return token(Terminals.GOTO); }
  "if"                           { return token(Terminals.IF); }
  "public"                       { return token(Terminals.PUBLIC); }
  "short"                        { return token(Terminals.SHORT); }
  "super"                        { return token(Terminals.SUPER); }
  "switch"                       { return token(Terminals.SWITCH); }
  "synchronized"                 { return token(Terminals.SYNCHRONIZED); }
  "package"                      { return token(Terminals.PACKAGE); }
  "private"                      { return token(Terminals.PRIVATE); }
  "protected"                    { return token(Terminals.PROTECTED); }
  "transient"                    { return token(Terminals.TRANSIENT); }
  "return"                       { return token(Terminals.RETURN); }
  "void"                         { return token(Terminals.VOID); }
  "static"                       { return token(Terminals.STATIC); }
  "while"                        { return token(Terminals.WHILE); }
  "this"                         { return token(Terminals.THIS); }
  "throw"                        { return token(Terminals.THROW); }
  "throws"                       { return token(Terminals.THROWS); }
  "try"                          { return token(Terminals.TRY); }
  "volatile"                     { return token(Terminals.VOLATILE); }
  "strictfp"                     { return token(Terminals.STRICTFP); }

  /* boolean literals */
  "true"                         { return token(Terminals.BOOLEAN_LITERAL, new Boolean(true)); }
  "false"                        { return token(Terminals.BOOLEAN_LITERAL, new Boolean(false)); }

  /* null literal */
  "null"                         { return token(Terminals.NULL_LITERAL); }


  /* separators */
  "("                            { return token(Terminals.LPAREN); }
  ")"                            { return token(Terminals.RPAREN); }
  "{"                            { return token(Terminals.LBRACE); }
  "}"                            { return token(Terminals.RBRACE); }
  "["                            { return token(Terminals.LBRACK); }
  "]"                            { return token(Terminals.RBRACK); }
  ";"                            { return token(Terminals.SEMICOLON); }
  ","                            { return token(Terminals.COMMA); }
  "."                            { return token(Terminals.DOT); }

  /* operators */
  "="                            { return token(Terminals.EQ); }
  ">"                            { return token(Terminals.GT); }
  "<"                            { return token(Terminals.LT); }
  "!"                            { return token(Terminals.NOT); }
  "~"                            { return token(Terminals.COMP); }
  "?"                            { return token(Terminals.QUESTION); }
  ":"                            { return token(Terminals.COLON); }
  "=="                           { return token(Terminals.EQEQ); }
  "<="                           { return token(Terminals.LTEQ); }
  ">="                           { return token(Terminals.GTEQ); }
  "!="                           { return token(Terminals.NOTEQ); }
  "&&"                           { return token(Terminals.ANDAND); }
  "||"                           { return token(Terminals.OROR); }
  "++"                           { return token(Terminals.PLUSPLUS); }
  "--"                           { return token(Terminals.MINUSMINUS); }
  "+"                            { return token(Terminals.PLUS); }
  "-"                            { return token(Terminals.MINUS); }
  "*"                            { return token(Terminals.MULT); }
  "/"                            { return token(Terminals.DIV); }
  "&"                            { return token(Terminals.AND); }
  "|"                            { return token(Terminals.OR); }
  "^"                            { return token(Terminals.XOR); }
  "%"                            { return token(Terminals.MOD); }
  "<<"                           { return token(Terminals.LSHIFT); }
  ">>"                           { return token(Terminals.RSHIFT); }
  ">>>"                          { return token(Terminals.URSHIFT); }
  "+="                           { return token(Terminals.PLUSEQ); }
  "-="                           { return token(Terminals.MINUSEQ); }
  "*="                           { return token(Terminals.MULTEQ); }
  "/="                           { return token(Terminals.DIVEQ); }
  "&="                           { return token(Terminals.ANDEQ); }
  "|="                           { return token(Terminals.OREQ); }
  "^="                           { return token(Terminals.XOREQ); }
  "%="                           { return token(Terminals.MODEQ); }
  "<<="                          { return token(Terminals.LSHIFTEQ); }
  ">>="                          { return token(Terminals.RSHIFTEQ); }
  ">>>="                         { return token(Terminals.URSHIFTEQ); }

  /* string literal */
  \"                             { yybegin(STRING); string.setLength(0); }

  /* character literal */
  \'                             { yybegin(CHARLITERAL); }

  /* numeric literals */

  {DecIntegerLiteral}            { return token(Terminals.INTEGER_LITERAL, Integer.valueOf(yytext())); }
  {DecLongLiteral}               { return token(Terminals.INTEGER_LITERAL, Long.valueOf(yytext().substring(0,yylength()-1))); }

  {HexIntegerLiteral}            { return token(Terminals.INTEGER_LITERAL, Integer.valueOf(yytext().substring(2),16)); }
  {HexLongLiteral}               { return token(Terminals.INTEGER_LITERAL, Long.valueOf(yytext().substring(2,yylength()-1),16)); }

  {OctIntegerLiteral}            { return token(Terminals.INTEGER_LITERAL, Integer.valueOf(yytext(),8)); }
  {OctLongLiteral}               { return token(Terminals.INTEGER_LITERAL, Long.valueOf(yytext().substring(0,yylength()-1),8)); }

  {FloatLiteral}                 { return token(Terminals.FLOATING_POINT_LITERAL, Float.valueOf(yytext().substring(0,yylength()-1))); }
  {DoubleLiteral}                { return token(Terminals.FLOATING_POINT_LITERAL, Double.valueOf(yytext())); }
  {DoubleLiteral}[dD]            { return token(Terminals.FLOATING_POINT_LITERAL, Double.valueOf(yytext().substring(0,yylength()-1))); }

  /* comments */
  {Comment}                      { /* ignore */ }

  /* whitespace */
  {WhiteSpace}                   { /* ignore */ }

  /* identifiers */
  {Identifier}                   { return token(Terminals.IDENTIFIER, yytext()); }
}

<STRING> {
  \"                             { yybegin(YYINITIAL); return token(Terminals.STRING_LITERAL, string.toString()); }

  {StringCharacter}+             { string.append( yytext() ); }

  /* escape sequences */
  "\\b"                          { string.append( '\b' ); }
  "\\t"                          { string.append( '\t' ); }
  "\\n"                          { string.append( '\n' ); }
  "\\f"                          { string.append( '\f' ); }
  "\\r"                          { string.append( '\r' ); }
  "\\\""                         { string.append( '\"' ); }
  "\\'"                          { string.append( '\'' ); }
  "\\\\"                         { string.append( '\\' ); }
  \\[0-3]?{OctDigit}?{OctDigit}  { char val = (char) Integer.parseInt(yytext().substring(1),8); string.append( val ); }

  /* error cases */
  \\.                            { throw new Scanner.Exception(yyline + 1, yycolumn + 1, "Illegal escape sequence \""+yytext()+"\""); }
  {LineTerminator}               { throw new Scanner.Exception(yyline + 1, yycolumn + 1, "Unterminated string at end of line"); }
}

<CHARLITERAL> {
  {SingleCharacter}\'            { yybegin(YYINITIAL); return token(Terminals.CHARACTER_LITERAL, new Character(yytext().charAt(0))); }

  /* escape sequences */
  "\\b"\'                        { yybegin(YYINITIAL); return token(Terminals.CHARACTER_LITERAL, new Character('\b'));}
  "\\t"\'                        { yybegin(YYINITIAL); return token(Terminals.CHARACTER_LITERAL, new Character('\t'));}
  "\\n"\'                        { yybegin(YYINITIAL); return token(Terminals.CHARACTER_LITERAL, new Character('\n'));}
  "\\f"\'                        { yybegin(YYINITIAL); return token(Terminals.CHARACTER_LITERAL, new Character('\f'));}
  "\\r"\'                        { yybegin(YYINITIAL); return token(Terminals.CHARACTER_LITERAL, new Character('\r'));}
  "\\\""\'                       { yybegin(YYINITIAL); return token(Terminals.CHARACTER_LITERAL, new Character('\"'));}
  "\\'"\'                        { yybegin(YYINITIAL); return token(Terminals.CHARACTER_LITERAL, new Character('\''));}
  "\\\\"\'                       { yybegin(YYINITIAL); return token(Terminals.CHARACTER_LITERAL, new Character('\\')); }
  \\[0-3]?{OctDigit}?{OctDigit}\' { yybegin(YYINITIAL);
			                              int val = Integer.parseInt(yytext().substring(1,yylength()-1),8);
			                            return token(Terminals.CHARACTER_LITERAL, new Character((char)val)); }

  /* error cases */
  \\.                            { throw new Scanner.Exception(yyline + 1, yycolumn + 1, "Illegal escape sequence \""+yytext()+"\""); }
  {LineTerminator}               { throw new Scanner.Exception(yyline + 1, yycolumn + 1, "Unterminated character literal at end of line"); }
}

/* error fallback */
.|\n                             { throw new Scanner.Exception(yyline + 1, yycolumn + 1, "unrecognized character '" + yytext() + "'"); }
