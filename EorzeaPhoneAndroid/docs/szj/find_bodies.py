"""For the endpoints whose body is an opaque variable, find the call sites that
build that object and print surrounding context so the field names can be read off.
"""
import os, re, sys

root = 'C:/Users/Administrator/AppData/Local/Temp/rs_js'
main = 'C:/Users/Administrator/AppData/Local/Temp/rs_main.js'

targets = sys.argv[1:] or [
    'posts/comment', 'posts/like', 'posts/star', 'posts/relay',
    'responseRecruitFb', 'responseRecruitGuild', 'responseRecruitOther',
    'responseNoviceEntertain',
]

files = [main] + [os.path.join(root, f) for f in sorted(os.listdir(root))]

for t in targets:
    print('=' * 78)
    print('ENDPOINT:', t)
    print('=' * 78)
    # 1. find the wrapper function name that owns this url, per file
    for p in files:
        txt = open(p, encoding='utf-8', errors='replace').read()
        if t not in txt:
            continue
        name = os.path.basename(p)
        for m in re.finditer(r'function\s+(\w+)\s*\([^)]*\)\s*\{\s*return\s+\w+\(\{\s*url:[^}]*?' + re.escape(t) + r'"', txt):
            fn = m.group(1)
            print('  [%s] wrapper fun %s' % (name, fn))
            # find the export alias:  export{...  fn as X ...}
            for em in re.finditer(r'export\s*\{([^}]*)\}', txt):
                for pair in em.group(1).split(','):
                    pair = pair.strip()
                    am = re.match(r'^' + fn + r'\s+as\s+(\w+)$', pair)
                    if am:
                        print('      exported as: %s' % am.group(1))
                    elif pair == fn:
                        print('      exported as: %s (same name)' % fn)
    print()
