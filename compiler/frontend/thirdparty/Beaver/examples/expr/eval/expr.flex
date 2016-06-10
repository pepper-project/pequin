package expr.eval;

import beaver.Symbol;
import beaver.Scanner;

import expr.eval.ExpressionParser.Terminals;

%%

%class ExpressionScanner
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

Number = [:digit:] [:digit:]*

%%

{WhiteSpace}+   { /* ignore */ }

<YYINITIAL> {
	{Number}    { return newToken(Terminals.NUMBER, new Double(yytext())); }

	"("         { return newToken(Terminals.LPAREN); }
	")"         { return newToken(Terminals.RPAREN); }
	"*"         { return newToken(Terminals.MULT);   }
	"/"         { return newToken(Terminals.DIV);    }
	"+"         { return newToken(Terminals.PLUS);   }
	"-"         { return newToken(Terminals.MINUS);  }
}

.|\n            { throw new Scanner.Exception("unexpected character '" + yytext() + "'"); }
