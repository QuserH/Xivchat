"""Given (module, exportAlias), find files importing it and print the call with its
object literal argument, so the request body field names can be read directly.
"""
import os, re, sys

root = 'C:/Users/Administrator/AppData/Local/Temp/rs_js'
main = 'C:/Users/Administrator/AppData/Local/Temp/rs_main.js'
files = [main] + [os.path.join(root, f) for f in sorted(os.listdir(root))]

# (module filename substring, exported alias, label)
targets = [
    ('posts.PdYvHlRm.js', 'h', 'posts/comment'),
    ('posts.PdYvHlRm.js', 'P', 'posts/like'),
    ('posts.PdYvHlRm.js', 'e', 'posts/star'),
]
if len(sys.argv) >= 4:
    targets = [(sys.argv[1], sys.argv[2], sys.argv[3])]


def balanced(txt, i):
    """Return the substring of the (...) group starting at index i (txt[i] == '(')."""
    depth = 0
    for j in range(i, min(len(txt), i + 4000)):
        c = txt[j]
        if c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
            if depth == 0:
                return txt[i:j + 1]
    return txt[i:i + 400]


for mod, alias, label in targets:
    print('=' * 78)
    print('%s   (exported as %s from %s)' % (label, alias, mod))
    print('=' * 78)
    hits = 0
    for p in files:
        txt = open(p, encoding='utf-8', errors='replace').read()
        base = os.path.basename(p)
        # find import statements pulling this module
        for im in re.finditer(r'import\s*\{([^}]*)\}\s*from\s*"[^"]*' + re.escape(mod) + r'"', txt):
            spec = im.group(1)
            local = None
            for pair in spec.split(','):
                pair = pair.strip()
                m = re.match(r'^' + re.escape(alias) + r'\s+as\s+(\w+)$', pair)
                if m:
                    local = m.group(1)
                elif pair == alias:
                    local = alias
            if not local:
                continue
            # find calls to the local name
            for cm in re.finditer(r'\b' + re.escape(local) + r'\s*\(', txt):
                arg = balanced(txt, cm.end() - 1)
                if '{' not in arg:
                    continue
                hits += 1
                print('  [%s] %s%s' % (base, local, arg[:600]))
                print()
    if hits == 0:
        print('  (no object-literal call sites found)')
    print()
