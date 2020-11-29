modnames = []
with open('ysyx_hbh.v') as f:
    for line in f:
        if line.find('module ') != -1:
            modname = line.split(' ')[1]
            modname = modname[:-2]
            modnames.append(modname)

with open('ysyx_hbh.v') as f:
    str = f.read()
    for modname in modnames:
        str = str.replace("module "+modname, "module hbh_"+modname)
        str = str.replace(" "+modname+" ", " hbh_"+modname+" ")
    print(str)
