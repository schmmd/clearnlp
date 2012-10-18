#!/usr/bin/python
import os
import sys
import glob

IN_DIR = sys.argv[1]
EXT    = sys.argv[2]

def getCounts(filename):
    fin = open(filename)
    sc  = 0
    wc  = 0
    vc  = 0
    
    for line in fin:
        l = line.split()
        if l:
            wc += 1
            if 'pb=' in l[4]: vc += 1
        else:
            sc += 1

    return (sc, wc, vc)

gt = [0, 0, 0]

for filename in glob.glob(os.path.join(IN_DIR, '*.'+EXT)):
    t = getCounts(filename)
    s = '%s %d %d %d' % (filename, t[0], t[1], t[2])
    print s
    
    gt[0] += t[0]
    gt[1] += t[1]
    gt[2] += t[2]

print gt