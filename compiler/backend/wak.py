import math
import random
import re
import collections
#waksman(width) returns hooked,  a list of switches annotated with target and source nodes. 
#in the following order: outermost leftswitches, outermost right switches, top subnetwork switches, bottom subnetwork switches.
#"network" prints the same as a recursively nested list of lists, for debugging

def waksman(n):
#    print "Waksman switch network of size %d\n" % (n)
    global swc
    swc = 0
    global varct
    varct = 0
    global hooked
    hooked = []
    inputs = mkNodes(n, "I")
    varct = 0
    outputs = mkNodes(n, "O")
    varct = 0

    buildwak(inputs, outputs, n)
    #print network
    #print hooked
    #print parseNet(hookedSwitches[0], hookedSwitches[1])
    return hooked

def parseNet(top, bot):
    topPieces = top.split()
    s1 = topPieces[0]
    t1 = topPieces[-1]
    sw = topPieces[2]
    bottomPieces = bot.split()
    s2 = bottomPieces[0]
    t2 = bottomPieces[-1]
    return (s1, s2, t1, t2, sw)

def buildwak(inputs, outputs, n):
    global hooked

    #base cases n = 1, 2: if n is 1, we have a duplicated node that needs replacing.
    if (n == 1):
        toRet = "%s == %s" % (inputs[0], outputs[0])
        leftPieces = hooked[-4].split()[0:-1]
        rightPiece = hooked[-2].split()[0]
        leftPieces.append(rightPiece)
        hooked[-4] = ' '.join(leftPieces)
        return toRet
    if (n == 2):
        sw = mkSwitches(1)
        toRet = [inputs[0] + ' -> ' +  sw[0] + ' -> ' + outputs[0], inputs[1] + ' -> ' +  sw[1] + ' -> ' + outputs[1]]
        hooked += toRet
        return toRet

    leftSwitchCount = int(math.floor(n/float(2)))
    rightSwitchCount = leftSwitchCount - 1 + (n % 2)
    (topSize, bottomSize) = (int(math.floor(n/float(2))), int(math.ceil(n/float(2))))

    leftSwitches = mkSwitches(leftSwitchCount)
    if (n % 2 == 1):
       leftSwitches.append("link")

    rightSwitches = mkSwitches(rightSwitchCount)
    if (n % 2 == 1):
        rightSwitches.append("link")
    else:
        rightSwitches.append("link")
        rightSwitches.append("link")

    leftSwitches = map(lambda input, switch: input + ' -> ' + switch, inputs, leftSwitches)
    rightSwitches = map(lambda switch, output: switch  + ' -> ' + output, rightSwitches, outputs)

    topLeftVars = []
    topRightVars = []
    botLeftVars = []
    botRightVars = []

    for i in range(n):
        if (leftSwitches[i].split()[2] == 'link'):
            var = leftSwitches[i].split()[0]
        else:
            var = mkNodes(1, "V")[0]
        leftSwitches[i] +=  ' -> ' + var
        if (leftSwitches[i].split()[2] != 'link'):
            hooked.append(leftSwitches[i])
        if (i % 2 == 1 or (n % 2 == 1 and i == n-1) ):
            botLeftVars.append(var)            
        else:
            topLeftVars.append(var)

    for i in range(n):
        if (rightSwitches[i].split()[0] == 'link'):
            var = rightSwitches[i].split()[-1]
        else:
            var = mkNodes(1, "V")[0]
        rightSwitches[i] = var + ' -> ' +  rightSwitches[i]
        if (rightSwitches[i].split()[2] != 'link'):
            hooked.append(rightSwitches[i])
        if (i % 2 == 1 or (n % 2 == 1 and i == n-1) ):
            botRightVars.append(var)            
        else:
            topRightVars.append(var)
                        
    topSub = buildwak(topLeftVars, topRightVars, topSize)
    botSub = buildwak(botLeftVars, botRightVars, bottomSize)
    
    return [leftSwitches, topSub, botSub, rightSwitches]

def mkSwitches(n):
    toRet = []
    global swc

    for i in range(n):
       toRet.append("Tsw%d" % (swc))
       toRet.append("Bsw%d" % (swc))
       swc += 1
    return toRet

def mkNodes(n, str):
   toRet = []
   global varct

   for i in range(n):
      toRet.append("%s%d" % (str, varct))
      varct += 1
   return toRet

def main():
#    waksman(10000)                                                                       
    waksman(12)
    waksman(9)
    waksman(8)
    waksman(7)
    waksman(6)
    waksman(5)
    waksman(4)
    waksman(3)
    waksman(2)
if __name__ == "__main__":
    main()
