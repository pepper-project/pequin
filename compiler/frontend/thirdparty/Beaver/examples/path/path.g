%package "path";
%class "PathParser";

%terminals NAME, COLON, SLASH, DOT;

%typeof NAME = "String";
%typeof filename, pathname, winpathname = "ArrayList";

%goal winpathname;
%goal pathname;
%goal filename;

winpathname
	= NAME.d COLON pathname.p		{: p.add(0, d); return new Symbol(p); :}
	;
	
pathname
	= SLASH filename.n				{: return new Symbol(n); :}
	| SLASH NAME.n pathname.p		{: p.add(0, n); return new Symbol(p); :}
	;
	
filename
	= NAME.n DOT NAME.e				{: ArrayList p = new ArrayList(); p.add(n); p.add(e); return new Symbol(p); :}
	;
name
	= NAME.n DOT NAME.e				{: ArrayList p = new ArrayList(); p.add(n); p.add(e); return new Symbol(p); :}
	;
