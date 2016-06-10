#!/usr/bin/python2
import inspect
import math
import os
import random
import sys
import re
import zcc_parser

INPUT_TAG = "INPUT"
OUTPUT_TAG = "OUTPUT"
CONSTRAINTS_TAG = "CONSTRAINTS"
VARIABLES_TAG = "VARIABLES"

END_TAG = "END_"
START_TAG = "START_"

m = 0  # input size
chi = 0 # number of constraints
prover_muls = 0 # number of multiplications the prover does to fill out the proof
prover_adds = 0 # number of multiplications the prover does to fill out the proof
prover_invs = 0 # number of multiplications the prover does to fill out the proof
NzABC = 0 # Number of nonzero elements in the A, B, and C matrices (combined)
input_vars = []  # input 
output_vars = []  # output 
variables = []  #variables 
proverBugginess = 0 # a number from 0 to 1 determining the probability of
		    # the prover skipping a proof variable.

def to_var(varname):
  if varname in input_vars:
    return input_vars[varname]
  elif varname in output_vars:
    return output_vars[varname]
  elif varname in variables:
    return variables[varname]
  elif varname[0].isdigit:
    constVar = {}
    constVar["name"] = varname;
    val = abs(int(varname));
    if val == 0: 
      constVar["na"] = 1;
    else:
      constVar["na"] = int(math.log(val,2) + 1);
    constVar["nb"] = 1;
    return constVar
  else:
    raise Exception("Key error: ", varname)

#converts, say, I0 to something like input[0]
def to_var_access(varname):
  if varname in input_vars:
    return "input_output_q[%d]" % (input_vars[varname]["index"])
  elif varname in output_vars:
    return "input_output_q[%d]" % (output_vars[varname]["index"])
  elif varname in variables:
    return "F1_q[ F1_index[%d] ]" % (variables[varname]["index"])
  else:
    return -1

#helper function for adding variables during the prover's computation
def prover_var(prefix, varName):
  var = zcc_parser.read_var("%s$%s" % (prefix, varName), variables, 0) # Possibly new variable

  return """
    mpq_t& %s = F1_q[ F1_index[%d] ];
""" % (varName, var["index"])


# Honest prover's implementation of not equal
def generate_computation_not_equals(arg0, arg1, target):
  #Create a new scope
  code = """
    { 
    """ 

  acX1 = to_var_access(arg0);
  if acX1 == -1:
     code += """
      mpq_t& X1 = temp_q2;
      mpq_set_str(X1, (char*) "%s", 10);
      """ % (arg0)
  else:
    code += """
      mpq_t& X1 = %s;
      """ % (acX1)

  acX2 = to_var_access(arg1);
  if acX2 == -1:
     code += """
      mpq_t& X2 = temp_q2;
      mpq_set_str(X2, (char*) "%s", 10);
      """ % (arg1)
  else:
    code += """
      mpq_t& X2 = %s;
      """ % (acX2)

  code += """
      mpq_t& Y = %s; 
    """ % (to_var_access(target))

  for varName in ["M"]:
    code += prover_var(target, varName)

  code += """
      int compare = mpq_cmp(X1, X2);
      if (compare == 0){
	mpq_set_ui(M, 0, 1);	
	mpq_set_ui(Y, 0, 1);	
      } else {
	mpq_sub(temp_q, X1, X2);
	//f(a,b)^-1 = b*a^-1
	mpz_invert(temp, mpq_numref(temp_q), prime);
	mpz_mul(temp, temp, mpq_denref(temp_q));
	mpq_set_z(M, temp);	
	mpq_set_ui(Y, 1, 1);	
      }
    """

  # End scope
  code += """
    }
    """ 

  return code;

# Honest prover's implementation of less than
def generate_computation_less(arg0, arg1, target):
  var0 = to_var(arg0)
  var1 = to_var(arg1)

  (na1, nb1) = zcc_parser.get_bits_diff(var0, arg0, var1, arg1);

  if (nb1 == 0):
    return generate_computation_less_i(na1, arg0, arg1, target)
  else:
    return generate_computation_less_f(na1, nb1, arg0, arg1, target)

def generate_computation_less_i(na2, arg0, arg1, target):

  #Create a new scope
  code = """
    { 
    """ 

  acY1 = to_var_access(arg0);
  if acY1 == -1:
     code += """
      mpq_t& Y1 = temp_q2;
      mpq_set_str(Y1, (char*) "%s", 10);
      mpq_canonicalize(Y1);
      """ % (arg0)
  else:
    code += """
      mpq_t& Y1 = %s;
      """ % (acY1)

  acY2 = to_var_access(arg1);
  if acY2 == -1:
     code += """
      mpq_t& Y2 = temp_q2;
      mpq_set_str(Y2, (char*) "%s", 10);
      mpq_canonicalize(Y2);
      """ % (arg1)
  else:
    code += """
      mpq_t& Y2 = %s;
      """ % (acY2)

  code += """
      mpq_t& Yout = %s; 
    """ % (to_var_access(target))

  for varName in ["Mlt", "Meq", "Mgt"]:
    code += prover_var(target, varName)

  code += """
      int compare = mpq_cmp(Y1, Y2);
      if (compare < 0){
	mpq_set_ui(Yout, 1, 1);
	mpq_set_ui(Mlt, 1, 1);
	mpq_set_ui(Meq, 0, 1);
	mpq_set_ui(Mgt, 0, 1);
	mpq_sub(temp_qs[0], Y1, Y2); 
      } else if (compare == 0){ 
	mpq_set_ui(Yout, 0, 1);
	mpq_set_ui(Mlt, 0, 1);
	mpq_set_ui(Meq, 1, 1);
	mpq_set_ui(Mgt, 0, 1);
	mpq_sub(temp_qs[0], Y1, Y2); 
      } else if (compare > 0){
	mpq_set_ui(Yout, 0, 1);
	mpq_set_ui(Mlt, 0, 1);
	mpq_set_ui(Meq, 0, 1);
	mpq_set_ui(Mgt, 1, 1);	
	mpq_sub(temp_qs[0], Y2, Y1); 
      }
    """

  code += """
      mpz_set_ui(temp, 1);
      mpz_mul_2exp(temp, temp, %d);
      mpz_add(temp, temp, mpq_numref(temp_qs[0]));
    """ % (na2 - 1)

  for i in range(0, na2-1): #0, 1, 2, ... na-2
    code += prover_var(target, "N%d" % (i))
    code += """
      mpz_tdiv_r_2exp(temp2, temp, 1);
      mpq_set_z(N%d, temp2);
      mpq_mul_2exp(N%d, N%d, %d);
      mpz_tdiv_q_2exp(temp, temp, 1); 
    """ % (i, i, i, i)

  # End scope
  code += """
    }
    """ 

  return code;


def generate_computation_less_f(na2, nb2, arg0, arg1, target):

  #XXX Currently we ignore the type system, and always use fixed-size less than gates. 
  #When the type system starts being more agressive, we will use its bounds instead.
  
  #na2 = 100;
  #nb2 = 32;
  #print "Warning - na=", na2, "/nb=", nb2," '<' gate being used, ignoring input type!";

  #Create a new scope
  code = """
    { 
    """ 

  acY1 = to_var_access(arg0);
  if acY1 == -1:
     code += """
      mpq_t& Y1 = temp_q2;
      mpq_set_str(Y1, (char*) "%s", 10);
      mpq_canonicalize(Y1);
      """ % (arg0)
  else:
    code += """
      mpq_t& Y1 = %s;
      """ % (acY1)

  acY2 = to_var_access(arg1);
  if acY2 == -1:
     code += """
      mpq_t& Y2 = temp_q2;
      mpq_set_str(Y2, (char*) "%s", 10);
      mpq_canonicalize(Y2);
      """ % (arg1)
  else:
    code += """
      mpq_t& Y2 = %s;
      """ % (acY2)

  code += """
      mpq_t& Yout = %s; 
    """ % (to_var_access(target))

  for varName in ["N", "D", "ND", "Mlt", "Meq", "Mgt"]:
    code += prover_var(target, varName)

  code += """
      int compare = mpq_cmp(Y1, Y2);
      if (compare < 0){
	mpq_set_ui(Yout, 1, 1);
	mpq_set_ui(Mlt, 1, 1);
	mpq_set_ui(Meq, 0, 1);
	mpq_set_ui(Mgt, 0, 1);
	mpq_sub(temp_q, Y1, Y2); 
	mpq_set_z(N, mpq_numref(temp_q));
	mpq_set_z(D, mpq_denref(temp_q)); //should be positive
      } else if (compare == 0){ 
	mpq_set_ui(Yout, 0, 1);
	mpq_set_ui(Mlt, 0, 1);
	mpq_set_ui(Meq, 1, 1);
	mpq_set_ui(Mgt, 0, 1);
	mpq_set_si(N, -1, 1);
	mpq_set_ui(D, 1, 1);
      } else if (compare > 0){
	mpq_set_ui(Yout, 0, 1);
	mpq_set_ui(Mlt, 0, 1);
	mpq_set_ui(Meq, 0, 1);
	mpq_set_ui(Mgt, 1, 1);	
	mpq_sub(temp_q, Y2, Y1); 
	mpq_set_z(N, mpq_numref(temp_q));
	mpq_set_z(D, mpq_denref(temp_q)); //should be positive
      }
    """

  code += """
      mpz_set_ui(temp, 1);
      mpz_mul_2exp(temp, temp, %d);
      mpz_add(temp, temp, mpq_numref(N));
    """ % (na2)

  for i in range(0, na2): #0, 1, 2, ... na-1
    code += prover_var(target, "N%d" % (i))
    code += """
      mpz_tdiv_r_2exp(temp2, temp, 1);
      mpq_set_z(N%d, temp2);
      mpq_mul_2exp(N%d, N%d, %d);
      mpz_tdiv_q_2exp(temp, temp, 1); 
    """ % (i, i, i, i)

  code += """
      mpz_set(temp, mpq_numref(D));
    """
  for i in range(0, nb2+1): #0, 1, 2, ... nb
    code += prover_var(target, "D%d" % (i))
    code += """
      mpz_tdiv_r_2exp(temp2, temp, 1);
      mpq_set_z(D%d, temp2);
      mpz_tdiv_q_2exp(temp, temp, 1); 
    """ % (i)

  code += """
      mpq_inv(D, D);  //Invert D
      mpq_mul(ND, N, D); 
      """ 
  # End scope
  code += """
    }
    """ 

  return code;

#Calls mpq_set
def cpp_set(a, b):
  return """
    mpq_set(%s, %s);
    """ % (a, b)

#Calls mpq_mul on the prover side
def cpp_mul(a, b):
  global prover_muls
  prover_muls = prover_muls + 1

  return """
    mpq_mul(%s, %s, %s);
    """ % (a, a, b)

#Calls mpq_sub on the prover side
def cpp_sub(a, b):
  global prover_adds
  prover_adds = prover_adds + 1

  return """
    mpq_sub(%s, %s, %s);
    """ % (a, a, b)

#Calls mpq_add on the prover side
def cpp_add(a, b):
  global prover_adds
  prover_adds = prover_adds + 1

  return """
    mpq_add(%s, %s, %s);
    """ % (a, a, b)

#Calls mpq_neg
def cpp_negate(a):
  return """
    mpq_neg(%s, %s);
    """ % (a,a)


#Calls mpq_div and mpq_set.
def generate_scaled_assignment(constant, target, val):
  code = cpp_set(target, val);
  if (constant != "1"):
    code += """
    mpq_set_str(temp_q, "%s", 10);
    mpq_div(%s, %s, temp_q);
    """ % (constant, target, target)
  return code

# Honest prover's implementation of a polynomial, passed in as a sequence of tokens
def generate_computation_poly(tokens, tmp_index):
  if (tmp_index > 15):
    raise Exception("Polynomial required more than 15 intermediate variables to compute - aborting.");

  code = ""

  poly_target = "temp_qs[%d]" % (tmp_index)
  term_target = "temp_qs[%d]" % (tmp_index+1)
 
  hasTerms = False 
  hasFactors = False
  negate = False
  isEmpty = True

  while tokens != []:
    token = tokens.pop(0)
    access_var = to_var_access(token)

    if (token == "+" or token == "-"):
      if (hasFactors):	
	if (negate):
	  code += cpp_negate(term_target)
	if (hasTerms):
	  code += cpp_add(poly_target, term_target)
	else:
	  code += cpp_set(poly_target, term_target)
	negate = False	
	hasFactors = False
	hasTerms = True
	isEmpty = False
    
    if (token == "("):
      # Recurse
      code += generate_computation_poly(tokens, tmp_index + 2)
      recurse_var = "temp_qs[%d]" % (tmp_index + 2)
      # Add the factor
      if (not hasFactors):
	code += cpp_set(term_target, recurse_var)
      else:
	code += cpp_mul(term_target, recurse_var)
      hasFactors = True
    elif (token == ")"):
      break # Computation complete
    elif (token == "+"):
      pass
    elif (token == "*"): 
      pass 
    elif (token == "-"):
      negate = not negate
    elif (access_var == -1):
      #constant
      code += """
    mpq_set_str(temp_q, (char*)"%s", 10);
	""" % (token)
      # Add the factor
      if (not hasFactors):
	code += cpp_set(term_target, "temp_q")
      else:
	code += cpp_mul(term_target, "temp_q")
      hasFactors = True
    else:
      #not constant
      if (not hasFactors):
	code += cpp_set(term_target, access_var)
      else:
	code += cpp_mul(term_target, access_var)
      hasFactors = True

  #Emit last term
  if (hasFactors):	
    if (negate):
      code += cpp_negate(term_target)
    if (hasTerms):
      code += cpp_add(poly_target, term_target)
    else:
      code += cpp_set(poly_target, term_target)
    isEmpty = False

  if (isEmpty):
    code += """
    mpq_set_ui(%s, 0, 1);
    """ % (poly_target)

  return code

#Given a list of tokens that starts with a (, pop(0) tokens from tokens until the matching ) is found. Pop that matching paren.
#All tokens between the parenthesis are returned as a list.
def read_poly(tokens):
  toRet = []
  nesting = 1
  if (tokens.pop(0) != "("):
    raise Exception("Format error")
  while(nesting > 0):
    token = tokens.pop(0)
    if (token == ")"):
      nesting = nesting-1
    elif (token == "("):
      nesting = nesting+1
      
    toRet += [token]
  
  toRet.pop(); #Dont return the last )
  return toRet
    

def generate_computation_line(line):
  global prover_muls
  global prover_adds
  global prover_invs

  #determine what kind of computation is taking place
  if ("!=" in line):
    prover_invs = prover_invs + 1
    terms = re.split("\\s+",line)
    return generate_computation_not_equals(terms[0], terms[2], terms[4]);
  elif ("<" in line):
    prover_adds = prover_adds + 1
    terms = re.split("\\s+",line)
    return generate_computation_less(terms[0], terms[2], terms[4]);
  else:
    tokens = re.split("\\s+", line)
    # Depends on whether we have zaatar or ginger constraints 
    if (zcc_parser.framework == "GINGER"):
      (constant, target) = get_poly_output(tokens)
      code = generate_computation_poly(tokens, 0)
      code += generate_scaled_assignment(constant, target, "temp_qs[0]")
      return code
    if (zcc_parser.framework == "ZAATAR"):    
      polyA = read_poly(tokens)
      star = tokens.pop(0)
      if (star != "*"):
	raise Exception("Format error")
      polyB = read_poly(tokens)
      plus = tokens.pop(0)
      if (plus != "+"):
	raise Exception("Format error")
      polyC = read_poly(tokens) 
      (constant, target) = get_poly_output(polyC)
      if (polyA == []):
	if (polyB != []):
	  raise Exception("Either A and B are both empty, or neither is empty")
	code = generate_computation_poly(polyC, 0)
	code += generate_scaled_assignment(constant, target, "temp_qs[0]")
	return code
      else:
	if (polyB == []):
	  raise Exception("Either A and B are both empty, or neither is empty")
        code = ""
        code += generate_computation_poly(polyA, 0)
        code += generate_computation_poly(polyB, 1)
        code += generate_computation_poly(polyC, 2)
	code += cpp_mul("temp_qs[0]", "temp_qs[1]")
	code += cpp_add("temp_qs[0]", "temp_qs[2]")
	code += generate_scaled_assignment(constant, target, "temp_qs[0]")
	return code 
 
# For a polynomial constraint such as x1 * x2 - 4 * x3, returns ("4", x3)
# For a polynomial constraint x1 * x2 - x3, returns ("1", x3)
def get_poly_output(tokens): 
  target = to_var_access(tokens.pop())
  constant = "1"
  if (tokens[-1] == "*"):
    tokens.pop()
    constant = tokens.pop()
  if (tokens[-1] != "-"):
    raise Exception("Polynomial expression didn't end with - (some variable), to provide an output variable")
  tokens.pop(); # -
  return (constant, target)

def generate_computation(spec_file):
  code = """
    gmp_printf("Running computation\\n");
    """

  def f(line):
    #Shortcircuiting.
    if (line.startswith("shortcircuit")):
      terms = line.split(" ");
      code += """
    mpq_set_str(temp_q, (char *)"%s", 10);
    if (!mpq_equal(temp_q, %s)){ //Shortcircuiting condition
      """ % (terms[5], to_var_access(terms[3]))

    elif (line.startswith("}")):
      code += """
      }
      """
    else:
      computationForLine = generate_computation_line(line)

      if (random.random() < float(proverBugginess)):
        code += """
        #ifndef BUGGY_PROVER
        //parsing %s
        %s
        #endif
	      """ % (line, computationForLine)
      else:
        code += """
          //parsing %s
          %s
	""" % (line, computationForLine)

  code += """
  // convert output_q to output
  convert_to_z(output_size, output, output_q, prime);

  // convert F1_q to F1
  convert_to_z(num_vars, F1, F1_q, prime);
  """;  

  zcc_parser.process_spec_section(spec_file, START_TAG + CONSTRAINTS_TAG, END_TAG + CONSTRAINTS_TAG, f)

#  if (zcc_parser.printMetrics):
#    print("""
#metric_num_prover_add %s %d
#metric_num_prover_mul %s %d
#metric_num_prover_inv %s %d
#    """ % (zcc_parser.classname, prover_adds,c zcc_parser.classname, prover_muls, zcc_parser.classname, prover_invs))

  return code

