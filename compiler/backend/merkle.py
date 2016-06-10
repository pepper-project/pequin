#!/bin/python2

import subprocess
import tempfile

class ConsEntry(object):
  
  def __init__(self, key):
    self.key = key
    self.tmpls = {}
    self.external_vars = []
    self.num_internal_vars = 0
    self.num_constraints = 0
    self.Aij = 0
    self.Bij = 0
    self.Cij = 0
    
class SubstEntry(object):
  def __init__(self):
    self.table = {}
    self.internal_var_offset = 0

class MerkleConsGen(object):
  
  def __init__(self, hash_type, verbose):
    self.hash_type = hash_type
#     self.hash_sizes = {"null" : 4, "ggh": 12}
    self.hash_sizes = {"null" : 4, "ggh": 19}
    
    self.cons = {}
    self.subst_entries = {}
    self.verbose = verbose
  
  def generate_get(self, db_size):
    return self.generate_op("get", db_size, 1)
  
  def generate_get_bits(self, db_size, num_val_vars):
    return self.generate_op("get_bits", db_size, num_val_vars)
  
  def generate_put(self, db_size):
    return self.generate_op("put", db_size, 1)
  
  def generate_put_bits(self, db_size, num_val_vars):
    return self.generate_op("put_bits", db_size, num_val_vars)  
  
  def generate_get_block_by_hash(self, num_val_vars):
    return self.generate_op("get_block_by_hash", 4, num_val_vars)  
  
  def generate_put_block_by_hash(self, num_val_vars):
    return self.generate_op("put_block_by_hash", 4, num_val_vars)  
  
  def generate_free_block_by_hash(self):
    return self.generate_op("free_block_by_hash", 4, 0)  
  
      
  def generate_op(self, op, db_size, num_val_vars):
    key = "state_op_%s_%s_%s_%s" % (op, db_size, self.hash_type, num_val_vars)
    try:
      return self.cons[key]
    except KeyError:
      entry = ConsEntry(key)
      
      for n in ["pws", "qapA", "qapB", "qapC"]:
        with make_temp_file(key, n) as f:
          entry.tmpls[n] = f.name
          f.close()
        
      subprocess.check_call(["../Gocode/bin/GenMerkle", "--op=%s" % op, "--dbSize=%d" % db_size, \
                            "--hashType=%s" % self.hash_type, "--numValueVars=%s" % num_val_vars, \
                            "--pwsFile=%s" % entry.tmpls["pws"], "--qapAFile=%s" % entry.tmpls["qapA"], \
                            "--qapBFile=%s" % entry.tmpls["qapB"], "--qapCFile=%s" % entry.tmpls["qapC"], "--outputTmpl=1"])

      with make_temp_file(key, "pws") as new_pws:
        extract_vars(entry.tmpls["pws"], entry, new_pws)
        entry.tmpls["pws"] = new_pws.name # Strip out the header
      
      self.cons[key] = entry
      return entry
    
  def get_cons_entry(self, key):
    return self.cons[key]
    
  def num_hash_elts(self):
    return self.hash_sizes[self.hash_type]
  
  def add_subst_entry(self, key, subst_entry):
    self.subst_entries[key] = subst_entry
    
  def del_subst_entry(self, key):
    del self.subst_entries[key]
    
  def get_subst_entry(self, key):
    return self.subst_entries[key]
  

def make_temp_file(key, name):
  return tempfile.NamedTemporaryFile(prefix="%s_%s_" % (key, name), suffix=".tmpl", delete=False)

def extract_vars(pws_tmpl, entry, new_pws):
  with open(pws_tmpl, "r") as f:
    in_ext_vars = False
    for line in f:
      line = line.strip()
      if not in_ext_vars:
          in_ext_vars = (line.startswith("EXTERNAL VARS:"))
      elif line == "":
        break
      else:
        entry.external_vars.append(line)
    
    for line in f:
      if line.startswith("NUM INTERNAL VARS:"):
        entry.num_internal_vars = int(line.split(":")[1])
      elif line.startswith("NUM CONSTRAINTS:"):
        entry.num_constraints = int(line.split(":")[1])
      elif line.startswith("Aij:"):
        entry.Aij = int(line.split(":")[1])
      elif line.startswith("Bij:"):
        entry.Bij = int(line.split(":")[1])
      elif line.startswith("Cij:"):
        entry.Cij = int(line.split(":")[1])
      elif line.startswith("*#"):
        break
      
    for line in f:
      new_pws.write(line)
  
  
  
  
