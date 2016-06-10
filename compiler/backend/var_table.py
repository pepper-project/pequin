class VarTable(object):
  
  def __init__(self, offset):
    self.named_vars = {}
    self.num_vars = 0;
    self.offset = offset
    
    # helper method for read_vars_section. Returns the created variable object
  def read_var(self, line):
    # handle comments
    line = line.split()
  
    if (line[0] in self.named_vars):
      return self.named_vars[line[0]]
  
    var_obj = {}
    var_obj["name"] = line[0]
    var_obj["index"] = (self.offset + self.num_vars) #get the next available index
  
    if (len(line) >= 2):
      var_obj["comment"] = line[1]
      var_obj["type"] = line[2]
      if (var_obj["type"] == "int"):
        var_obj["na"] = int(line[4])
        var_obj["nb"] = 0
      elif (var_obj["type"] == "float"):
        var_obj["na"] = int(line[4])
        var_obj["nb"] = int(line[6])
      elif (var_obj["type"] == "uint"):
        var_obj["na"] = int(line[4])
        var_obj["nb"] = 0
  
    # self.named_vars[line[0]] = var_obj
    self.named_vars[var_obj["name"]] = var_obj #Same behavior as above line
    self.num_vars += 1
    return var_obj
  
  def add_vars(self, num_new_vars):
    start_num = self.offset + self.num_vars
    self.num_vars += num_new_vars
    return start_num
    
