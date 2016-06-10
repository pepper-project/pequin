package path;

import beaver.Symbol;
import beaver.Scanner;

import path.PathParser.Terminals;

%%

%class PathScanner
%extends Scanner
%function nextToken
%type Symbol
%yylexthrow Scanner.Exception
%eofval{
	return newToken(Terminals.EOF, "end-of-file");
%eofval}
%unicode
%line
%column
%{
	private Symbol newToken(short id)
	{
		return new Symbol(id, yyline + 1, yycolumn + 1, yylength());
	}

	private Symbol newToken(short id, Object value)
	{
		return new Symbol(id, yyline + 1, yycolumn + 1, yylength(), value);
	}
%}
LineTerminator = \r|\n|\r\n
WhiteSpace     = {LineTerminator} | [ \t\f]

%%

{WhiteSpace}+          { /* ignore */ }

[A-Za-z_][0-9A-Za-z_]* { return newToken(Terminals.NAME, yytext()); }

"."                    { return newToken(Terminals.DOT);   }
"/"         		   { return newToken(Terminals.SLASH); }
":"                    { return newToken(Terminals.COLON); }

.|\n                   { throw new Scanner.Exception("unexpected character '" + yytext() + "'"); }
