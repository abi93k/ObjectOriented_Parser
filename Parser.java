import java.util.*;
/* OBJECT-ORIENTED PARSER FOR TINYPL
 * program ->  decls stmts end
 * decls   ->  int idlist ';'
 * idlist  ->  id [',' idlist ]
 * stmts   ->  stmt [ stmts ]
 * stmt    ->  assign ';'| cmpd | cond | loop
 * assign  ->  id '=' expr
 * cmpd    ->  '{' stmts '}'
 * cond    ->  if '(' rexp ')' stmt [ else stmt ]
 * loop    ->  for '(' [assign] ';' [rexp] ';' [assign] ')' stmt
 * rexp    ->  expr ('<' | '>' | '==' | '!= ') expr
 * expr    ->  term   [ ('+' | '-') expr ]
 * term    ->  factor [ ('*' | '/') term ]
 * factor  ->  int_lit | id | '(' expr ')'
 */

public class Parser {

	public static void main(String[] args) {


		System.out.println("Enter a program, terminate with end! \n");
		Lexer.lex();
		new Program();
		Code.output();
	}
}

class Program { // program ->  decls stmts end
	
	Decls d;
	Stmts s;
	public static  HashMap<Character, Integer> identifiers = new HashMap<Character, Integer>(); // For associating identifiers with an integer 
	public static int id_count=1; // indicates the number of identifiers in the program
	
	public Program(){
		d=new Decls();
		s=new Stmts();

		if(Lexer.nextToken == Token.KEY_END){ 
			Code.gen("return");
		}
	}
}



class Decls { // decls   ->  int idlist ';'
	Idlist ids;
	Decls d;

	public Decls(){
		ids = new Idlist();
		Lexer.lex(); // skip semicolon

	}

}

class Idlist { // idlist  ->  id [',' idlist ]
	Idlist id;

	public Idlist(){

		Lexer.lex(); // skip 'int' 
		if(Lexer.nextToken==Token.ID){

			Program.identifiers.put(Lexer.ident,Program.id_count++); // Mapping identifiers to numbers

			Lexer.lex();
			if(Lexer.nextToken==Token.COMMA){ 

				id=new Idlist();


			}
		}
	}
}


class Stmts { // stmts   ->  stmt [ stmts ]

	Stmts stmts;
	Stmt stmt;

	public Stmts(){

		stmt = new Stmt();

		if((Lexer.nextToken != Token.RIGHT_BRACE) && Lexer.nextToken != Token.KEY_END){

			stmts = new Stmts();
		}

	}
}
class Stmt { // stmt    ->  assign ';'| cmpd | cond | loop

	Assign assign;
	Cond cond;
	Loop loop;
	Cmpd cmpd;
	public Stmt() {

		if(Lexer.nextToken == Token.ID){
			assign=new Assign();

			if(Lexer.nextToken==Token.SEMICOLON){
				Lexer.lex(); // skip semicolon 
			}
		}

		else if(Lexer.nextToken == Token.KEY_IF) {
			cond= new Cond();			
		}

		else if(Lexer.nextToken== Token.LEFT_BRACE){
			cmpd=new Cmpd();
		}
		else if(Lexer.nextToken== Token.KEY_FOR){
			loop=new Loop();
		}

	}


}

class Assign{ // assign  ->  id '=' expr
	public static char identifier;
	Expr e;
	int index;
	public Assign(){
		if(Lexer.nextToken==Token.ID){
			identifier=Lexer.ident; // save the identifier
			Lexer.lex(); // get '=' operator

		}
		if(Lexer.nextToken==Token.ASSIGN_OP){ 

			Lexer.lex(); 
			e = new Expr();
			Lexer.lex();
			

			index=Program.identifiers.get(identifier); // get the integer associated with the identifier

			// save expression to the identifier

			if(index<=3){
				//destination identifier is implicit in the name of the instruction
				Code.gen("istore_"+Integer.toString(index));

			}
			else{
				Code.gen("istore "+Integer.toString(index));
				// allocate 1 byte for destination identifier 
				Code.codeptr=Code.codeptr+1;
			}


		}

	}
}




class Cmpd { // cmpd    ->  '{' stmts '}'

	Stmts stmts;

	public Cmpd() {
		if(Lexer.nextToken == Token.LEFT_BRACE) {
			Lexer.lex();

			stmts = new Stmts() ;

		}
		if (Lexer.nextToken == Token.RIGHT_BRACE){
			Lexer.lex();
		}
	}
}


class Rexp { // rexp    ->  expr ('<' | '>' | '==' | '!= ') expr 


	Expr e1,e2;
	char op;

	public static ArrayList<Integer>  if_pointers=new ArrayList<Integer>();
	
	public static int is_assign;

	public Rexp(){

		e1=new Expr();

		if(Lexer.nextToken== Token.LESSER_OP || Lexer.nextToken== Token.GREATER_OP || Lexer.nextToken== Token.EQ_OP || Lexer.nextToken== Token.NOT_EQ ){
			op=Lexer.nextChar;
			Lexer.lex();
			e2 = new Expr();
			Code.gen(Code.opcode(op));

			Rexp.if_pointers.add(Code.getCodeptr()-1);
			Code.codeptr=Code.codeptr+2; // allocate 2 bytes
			
			

		}
		
		
		
		
	}
}


class Cond { // cond    ->  if '(' rexp ')' stmt [ else stmt ]

	Stmt s1;
	Stmt s2;
	Rexp r;

	public Cond(){
		int go_to_pointer;
		int location;

		Lexer.lex(); // skip 'if' keyword

		if(Lexer.nextToken == Token.LEFT_PAREN) {
			Lexer.lex(); // skip left parenthesis
			r=new Rexp();

		}
		if (Lexer.nextToken == Token.RIGHT_PAREN){
			Lexer.lex();
		}
		s1= new Stmt();
		if(Lexer.nextToken==Token.KEY_ELSE){
			Code.gen("goto ");
			go_to_pointer= Code.getCodeptr()-1;

			Code.codeptr=Code.codeptr+2; // allocate 2 bytes 
			location=Rexp.if_pointers.get(Rexp.if_pointers.size()-1);
			Code.code[location]=Code.code[location]+ " " + Code.codeptr;
			Rexp.if_pointers.remove(Rexp.if_pointers.size()-1);


			Lexer.lex();
			s2=new Stmt();
			Code.code[go_to_pointer] = Code.code[go_to_pointer] + " " + Code.codeptr;

		}
		else{
			location=Rexp.if_pointers.get(Rexp.if_pointers.size()-1);
			Code.code[location]=Code.code[location]+ " " + Code.codeptr;
			Rexp.if_pointers.remove(Rexp.if_pointers.size()-1);

		}

	}
}
class Loop { // loop    ->  for '(' [assign] ';' [rexp] ';' [assign] ')' stmt

	Rexp r;
	Assign a1,a2;
	Stmt s;
	ArrayList<String> buffer = new ArrayList<String>();
	int old_codeptr;
	int new_codeptr;
	int go_to_ptr;
	int location;
	int test=0; // indicates presence of test component
	

	public Loop(){

		Lexer.lex(); //skip 'for' keyword 

		if(Lexer.nextToken == Token.LEFT_PAREN) {
			Lexer.lex(); // skip left parenthesis


			if(Lexer.nextToken==Token.ID){ // check if initialization component is present

				a1=new Assign();
			}
			else{
				Lexer.lex(); //skip semicolon
			}

			go_to_ptr=Code.getCodeptr();

			if(Lexer.nextToken==Token.ID){ // check if test component is present
				test=1;

				r=new Rexp();

			}
				
			Lexer.lex(); // skip semicolon
			old_codeptr=Code.getCodeptr();

			

			if(Lexer.nextToken==Token.ID){ // check if increment component is present

				a2=new Assign();

				/* save increment code to temporary ArrayList - buffer  */
				
				new_codeptr=Code.getCodeptr()-1;

				for(int i=old_codeptr;i<=new_codeptr;i++){

					buffer.add(Code.code[i]);
				}

				/*rewind codeptr */
				Code.setCodeptr(new_codeptr-(new_codeptr-old_codeptr));



			}
			else{
				Lexer.lex(); // skip right parenthesis
			}
			
			
			s=new Stmt(); //body of loop

			/* add increment code */
			for (String code : buffer) {
				Code.gen(code);
			}

			Code.gen("goto "+go_to_ptr);
			Code.codeptr=Code.codeptr+2; // allocate 2 bytes
			if(test==1)
			{	
				location=Rexp.if_pointers.get(Rexp.if_pointers.size()-1);
				Code.code[location]=Code.code[location]+ " " + Code.codeptr;
				Rexp.if_pointers.remove(Rexp.if_pointers.size()-1);
			}


		}
	}
}







class Expr   { // expr    ->  term   [ ('+' | '-') expr ] 
	Term t;
	Expr e;
	char op;


	public Expr() {
		t = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			e = new Expr();
			Code.gen(Code.opcode(op));	 
		}
		

		
	}
}

class Term    { // term    ->  factor [ ('*' | '/') term ]

	Factor f;
	Term t;
	char op;




	public Term() {
		f = new Factor();

		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			t = new Term();
			Code.gen(Code.opcode(op));

		}
		


	}

}


class Factor { // factor  ->  int_lit | id | '(' expr ')'

	Expr e;
	int i;
	int index;
	Factor f;
	

	public Factor() {
		switch (Lexer.nextToken) {

		case Token.INT_LIT: // number
			i = Lexer.intValue;
			Code.gen(Code.intcode(i));
			if(i> 127)
				Code.codeptr=Code.codeptr+2; // allocate 2 bytes
			else if(i>5)
				Code.codeptr=Code.codeptr+1; // allocate 1 bytes
			Lexer.lex();
			break;
		case Token.ID:

			index=Program.identifiers.get(Lexer.ident);
			if(index<=3)
				Code.gen("iload_"+Integer.toString(index));
			else{
				Code.gen("iload "+Integer.toString(index));
				Code.codeptr=Code.codeptr+1; // allocate 1 byte
			}

			Lexer.lex();

			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();
			e=new Expr();
			Lexer.lex(); // skip over ')'
			break;

			
		default:
			break;
		}
	}
}


class Code { 
	static String[] code = new String[400];
	static int codeptr = 0;

	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}
	public static int getCodeptr() {
		return codeptr;
	}

	public static void setCodeptr(int val){
		codeptr=val;
	}


	public static String intcode(int i) {
		if (i > 127) {
			return "sipush " + i;

		}
		if (i > 5) {

			return "bipush " + i;
		}
		return "iconst_" + i;
	}

	public static String opcode(char op) {
		switch(op) {
		case '+' : return "iadd";
		case '-':  return "isub";
		case '*':  return "imul";
		case '/':  return "idiv";
		case '!':  return  "if_icmpeq";	
		case '>':  return  "if_icmple";	
		case '<':  return  "if_icmpge";
		case '=':  return  "if_icmpne";
		

		default: return "";
		}
	}

	public static void output() {
		for (int i=0; i<codeptr; i++)
			if(code[i]!=null)
				System.out.println(i+": "+code[i]);
	}
}


