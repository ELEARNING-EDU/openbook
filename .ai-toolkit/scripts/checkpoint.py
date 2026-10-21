#!/usr/bin/env python3
"""Read-only Git summary. Does not commit, stage, or dump source diffs."""
import subprocess
import sys
from pathlib import Path
root=Path(sys.argv[1] if len(sys.argv)>1 else '.').resolve()
for args in [('rev-parse','--show-toplevel'),('status','--short','--branch'),('log','-3','--oneline'),('diff','--stat'),('diff','--cached','--stat')]:
    r=subprocess.run(['git','-C',str(root),*args],capture_output=True,text=True)
    print('git '+' '.join(args)); print(r.stdout or r.stderr,end='')
    if r.returncode: sys.exit(r.returncode)
