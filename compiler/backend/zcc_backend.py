#!/usr/bin/python2

# import inspect
import os
import re
from optparse import OptionParser
import sys

from Cheetah.Template import Template
import zcc_parser

import merkle

# Go in apps_sfdl_gen/
PROVER_H = "_p.h"
VERIFIER_H = "_v.h" #Output in ginger
PROVER_IMPL = "_p.cpp"
VERIFIER_IMPL = "_v.cpp" #Output in ginger
CONSTANTS_H = "_cons.h"
CONSTANTS_IMPL = "_cons.cpp"

VERIFIER_INP_GEN_H = "_v_inp_gen.h"
VERIFIER_INP_GEN_IMPL = "_v_inp_gen.cpp"

# Go in bin/
F1_INDEX = ".f1index"
GAMMA12 = ".gamma12"
GAMMA0 = ".gamma0"
QAP = ".qap" #Output in zaatar
PROVER_WORKSHEET = ".pws" #Prover worksheet (output if in worksheet mode)


# Go in apps_sfdl_hw/
VERIFIER_INP_GEN_HW_H = "_v_inp_gen_hw.h"
VERIFIER_INP_GEN_HW_IMPL = "_v_inp_gen_hw.cpp"
PROVER_EXO_HW_H = "_p_exo.h"
PROVER_EXO_HW_IMPL = "_p_exo.cpp"

# Go in input_generation/ 
VERIFIER_LITE_INP_GEN_H = "_v_inp_gen.h"


#Directory that stores templates
DIR_TMPL = "templates/"

#Templates
CONSTANTS_H_TMPL = DIR_TMPL + "cons.h.tmpl"
CONSTANTS_IMPL_TMPL = DIR_TMPL + "cons.cc.tmpl"

PROVER_GINGER_H_TMPL = DIR_TMPL + "prover.ginger.h.tmpl"
PROVER_GINGER_CC_TMPL = DIR_TMPL + "prover.ginger.cc.tmpl"
PROVER_ZAATAR_H_TMPL = DIR_TMPL + "prover.zaatar.h.tmpl"
PROVER_ZAATAR_CC_TMPL = DIR_TMPL + "prover.zaatar.cc.tmpl"

VERIFIER_INP_GEN_HW_H_TMPL = DIR_TMPL + "verifier_inp_gen_hw.h.tmpl"
VERIFIER_INP_GEN_HW_CC_TMPL = DIR_TMPL + "verifier_inp_gen_hw.cc.tmpl"
PROVER_EXO_HW_H_TMPL = DIR_TMPL + "prover_exo.h.tmpl"
PROVER_EXO_HW_CC_TMPL = DIR_TMPL + "prover_exo.cc.tmpl"

VERIFIER_GINGER_H_TMPL = DIR_TMPL + "verifier.ginger.h.tmpl"
VERIFIER_GINGER_CC_TMPL = DIR_TMPL + "verifier.ginger.cc.tmpl"
VERIFIER_ZAATAR_H_TMPL = DIR_TMPL + "verifier.zaatar.h.tmpl"
VERIFIER_ZAATAR_CC_TMPL = DIR_TMPL + "verifier.zaatar.cc.tmpl"

VERIFIER_INP_GEN_H_TMPL = DIR_TMPL + "verifier_inp_gen.h.tmpl"
VERIFIER_INP_GEN_CC_TMPL = DIR_TMPL + "verifier_inp_gen.cc.tmpl"

MAIN_GINGER_TMPL = DIR_TMPL + "main.ginger.cc.tmpl"
MAIN_ZAATAR_TMPL = DIR_TMPL + "main.zaatar.cc.tmpl"

PARAMS_CLASS_H_TMPL = DIR_TMPL + "params.h.tmpl"
VERIFIER_LITE_INP_GEN = DIR_TMPL + "verifier_lite_inp_gen.h.tmpl"
VERIFIER_LITE_IMP_GEN_H_TMPL = DIR_TMPL + "verifier_lite_inp_gen_impl.h.tmpl"
class CodeGenerator():

  def __init__(self, output_dir, output_prefix, class_name, framework, worksheetMode, language):
    self.output_dir = output_dir
    self.output_prefix = output_prefix
    self.class_name = class_name
    zcc_parser.class_name = class_name
    zcc_parser.output_dir = output_dir
    self.framework = framework
    zcc_parser.framework = framework
    self.worksheetMode = worksheetMode
    self.language = language
    
  def write_to_file(self, name, contents):
    with open(os.path.join(self.output_dir,name), "w") as f:
      f.write(contents)

  def open_spec_file(self, spec_path):
    spec_file = open(spec_path, "r")

    zcc_parser.parse_spec_file(spec_file)

    if (zcc_parser.verbose):
      print ("Expanding database operations in " + spec_path)
    spec_file = zcc_parser.expand_db_ops_in_spec(spec_file)

    spec_file = zcc_parser.generate_memory_consistency_in_spec(spec_file)

    return spec_file

  def generate_pws(self, spec_file, defs):
    pws_loc = "bin/" + self.class_name +  PROVER_WORKSHEET
    pws_abs_loc = os.path.join(self.output_dir,pws_loc)
    if (zcc_parser.verbose):
      print ("Creating prover worksheet, result will appear at "+pws_loc)

    (defs['num_io_vars'], defs['num_z_vars']) = zcc_parser.generate_computation_worksheet(spec_file, pws_abs_loc) #leads to variable creation

    if (self.worksheetMode):
      defs['computation'] = zcc_parser.generate_computation_dynamic("bin/" + self.class_name + PROVER_WORKSHEET)
    else:
      defs['computation'] = zcc_parser.generate_computation_static(spec_file)

    spec_file.seek(0)

  def generate_matrices(self, spec_file, defs):
    #Generate the variable shuffling
    (f1_index, shuffledIndices) = zcc_parser.generate_F1_index()
    self.write_to_file("bin/" + self.class_name+F1_INDEX, f1_index)

    if (self.framework=="GINGER"):
      defs['NzA'] = "n/a"
      defs['NzB'] = "n/a"
      defs['NzC'] = "n/a"
    else:
      qap_file_name = os.path.join(self.output_dir, "bin/" + self.class_name + QAP);
      (defs['NzA'], defs['NzB'], defs['NzC'], defs['num_constraints']) = zcc_parser.generate_zaatar_matrices(spec_file, shuffledIndices, qap_file_name)

    spec_file.seek(0)
    
  def generate_prover(self, defs):
    if (self.framework=="GINGER"):
      t = Template(file=PROVER_GINGER_H_TMPL, searchList=[defs]) 
    else:
      t = Template(file=PROVER_ZAATAR_H_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + PROVER_H, t.__str__())

    if (self.framework=="GINGER"):
      defs['gamma12_file_name'] = "bin/" + self.class_name + GAMMA12;
      t = Template(file=PROVER_GINGER_CC_TMPL, searchList=[defs]) 
    else:
      t = Template(file=PROVER_ZAATAR_CC_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + PROVER_IMPL, t.__str__())

  def generate_input_generator(self, defs):
    defs['create_input'] = zcc_parser.generate_create_input()
    t = Template(file=VERIFIER_INP_GEN_H_TMPL, searchList=[defs])
    self.write_to_file(self.output_prefix + VERIFIER_INP_GEN_H, t.__str__())
    t = Template(file=VERIFIER_INP_GEN_CC_TMPL, searchList=[defs])
    self.write_to_file(self.output_prefix + VERIFIER_INP_GEN_IMPL, t.__str__())

  def generate_constants_file(self, spec_path, defs):
    defs['constants'] = zcc_parser.generate_constants(spec_path + ".cons")
    defs['is_mapred_comp'] = zcc_parser.generate_mapred_header(self.class_name)
    
    #if (self.language == "c"):
    #  (defs['type_def'], defs['compute_func_definition']) =  self.extract_clean_compute_function(self.class_name)
    #else:
    #  (defs['type_def'], defs['compute_func_definition']) =  ("", "")

    # t = Template(file=CONSTANTS_H_TMPL, searchList=[defs])
    # self.write_to_file(self.output_prefix + CONSTANTS_H, t.__str__())

    # t = Template(file=CONSTANTS_IMPL_TMPL, searchList=[defs])
    # self.write_to_file(self.output_prefix + CONSTANTS_IMPL, t.__str__())

  def protect_files(self, defs):
    protectedFiles = { (VERIFIER_LITE_INP_GEN_H, VERIFIER_LITE_IMP_GEN_H_TMPL)}
    #(PROVER_EXO_HW_H, PROVER_EXO_HW_H_TMPL), 
    # (PROVER_EXO_HW_IMPL, PROVER_EXO_HW_CC_TMPL),
    # (VERIFIER_INP_GEN_HW_H, VERIFIER_INP_GEN_HW_H_TMPL),
    # (VERIFIER_INP_GEN_HW_IMPL, VERIFIER_INP_GEN_HW_CC_TMPL)}

    for (targetfile, tmplfile) in protectedFiles:
      filename = "input_generation/" + self.class_name + targetfile;
      try:
        filename_ = os.path.join(self.output_dir, filename)
        with file(filename_, 'r'):
          os.utime(filename_, None) # Touch it if it exists
      except IOError:
        #The file doesn't exist, create it:
        t = Template(file=tmplfile, searchList=[defs])
        self.write_to_file(filename, t.__str__())

  def generate_verifier(self, spec_file, defs):
    if (self.framework=="GINGER"):
      self.write_ginger(defs, spec_file)
    else:
      self.write_zaatar(defs)

    spec_file.seek(0)

  def generate_code_from_template(self, spec_path):
    spec_file = self.open_spec_file(spec_path)

    defs = {}
    defs['computation_name'] = os.path.splitext(os.path.split(spec_path)[1])[0]
    defs['computation_classname'] = self.class_name
    defs['OUTPUT_PREFIX'] = re.sub(r'/',r'_',self.output_prefix).upper()
    defs['output_prefix'] = self.output_prefix

    self.generate_pws(spec_file, defs)

    #number of variables, number of basic constraints (chi) fixed from this point onwards

    self.generate_matrices(spec_file, defs)

    #Write the prover
    #self.generate_prover(defs)

    #Write the constants file
    self.generate_constants_file(spec_path, defs)

    self.protect_files(defs)

    #Create the input generator
    #self.generate_input_generator(defs)

    # Produce the verifier code (ginger) or A,B,C matrices (Zaatar) and drivers
    self.generate_verifier(spec_file, defs)

    spec_file.close()

  def extract_clean_compute_function(self, class_name):
    # this assumes that output_dir is the same as input_dir. Need to fix soon.
    filename = os.path.join("../", class_name + ".c")
    f = open(filename, 'r')
    content = f.readlines()
    f.close()
    typedefFlag = 1
    typedef = ""
    compute = ""
    # this is really hacky now. Will fix after SOSP.
    for line in content:
      if not line.startswith("#include"):
        if typedefFlag == 1:
          if line.startswith("//==========") or line.startswith("void compute("):
            typedefFlag = 0
            if line.startswith("void compute("):
              compute += line
          else:
            typedef += line
        else:
          compute += line

    return (typedef, compute)

  def write_ginger(self, defs, spec_file):
    #Write verifier's header
    t = Template(file=VERIFIER_GINGER_H_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + VERIFIER_H, t.__str__())

    #Write verifier's code
    gamma0 = zcc_parser.generate_gamma0(spec_file)
    gamma12 = zcc_parser.generate_gamma12(spec_file) #these routines generate chi
    defs['gamma12_file_name'] = "bin/" + self.class_name + GAMMA12;
    defs['gamma0_file_name'] = "bin/" + self.class_name + GAMMA0;
    t = Template(file=VERIFIER_GINGER_CC_TMPL, searchList=[defs])
    self.write_to_file(self.output_prefix + VERIFIER_IMPL, t.__str__())

    self.write_to_file("bin/"+self.class_name+GAMMA12, gamma12)
    self.write_to_file("bin/"+self.class_name+GAMMA0, gamma0)

    #Write the driver
    defs['comp_parameters'] = zcc_parser.generate_ginger_comp_params()
    t = Template(file=MAIN_GINGER_TMPL, searchList=[defs])     
    self.write_to_file(self.output_prefix + ".cpp", t.__str__()) 

  def write_zaatar(self, defs):
    #Write verifier's header
    # t = Template(file=VERIFIER_ZAATAR_H_TMPL, searchList=[defs])
    # self.write_to_file(self.output_prefix + VERIFIER_H, t.__str__())

    #Write verifier's code
    defs['load_qap'] = zcc_parser.generate_load_qap("bin/" + self.class_name + QAP)

    # t = Template(file=VERIFIER_ZAATAR_CC_TMPL, searchList=[defs])
    # self.write_to_file(self.output_prefix + VERIFIER_IMPL, t.__str__())
    
    t = Template(file=VERIFIER_LITE_INP_GEN, searchList=[defs])
    self.write_to_file("gen/input_gen.h", t.__str__())

    #Write the driver
    defs['comp_parameters'] = zcc_parser.generate_zaatar_comp_params()
    # t = Template(file=MAIN_ZAATAR_TMPL, searchList=[defs])
    # self.write_to_file(self.output_prefix + ".cpp", t.__str__())

    # t = Template(file=PARAMS_CLASS_H_TMPL, searchList=[defs])
    # self.write_to_file(self.output_prefix + "_params.h", t.__str__())
    
def main():
  parser = OptionParser()
  parser.add_option("-c", "--classname", dest="classname")
  parser.add_option("-s", "--spec_file", dest="spec")
  parser.add_option("-o", "--output_prefix", dest="output_prefix")
  parser.add_option("-d", "--output_dir", dest="output_dir", default=".")
  parser.add_option("-b", "--bugginess", dest="bugginess", default= 0)
  parser.add_option("-t", "--framework", dest="framework", default="GINGER")
  parser.add_option("-m", "--metrics", dest="metrics", default=0)
  parser.add_option("-w", "--worksheetMode", dest="worksheetMode", default=1)
  parser.add_option("--db-hash-func", dest="dbHashFunc", default="ggh")
  parser.add_option("--db-num-addresses", dest="dbNumAddresses", default="16")
  parser.add_option("--ram-cell-num-bits", dest="ramCellNumBits", default="1024")
  parser.add_option("--fast-ram-word-width", dest="fastRAMWordWidth", default="64")
  parser.add_option("--fast-ram-address-width", dest="fastRAMAddressWidth", default="32")
  parser.add_option("--language", dest="language", default="c")
  (opt, _) = parser.parse_args()
  
  mandatories = ['output_prefix', 'classname', 'spec', 'framework']
  for m in mandatories:
    if not opt.__dict__[m]:
        parser.print_help()
        exit(-1)
  zcc_parser.printMetrics = int(opt.metrics)
  zcc_parser.proverBugginess = float(opt.bugginess)

  zcc_parser.merkle_gen = merkle.MerkleConsGen(opt.dbHashFunc, zcc_parser.verbose)
  zcc_parser.db_size = int(opt.dbNumAddresses)
  zcc_parser.ram_cell_num_bits = int(opt.ramCellNumBits)
  zcc_parser.word_width = int(opt.fastRAMWordWidth)
  zcc_parser.address_width = int(opt.fastRAMAddressWidth)

  gen = CodeGenerator(opt.output_dir, opt.output_prefix, opt.classname,	
      opt.framework, int(opt.worksheetMode), opt.language)
  gen.generate_code_from_template(opt.spec)
  
if __name__ == "__main__":
  main()
