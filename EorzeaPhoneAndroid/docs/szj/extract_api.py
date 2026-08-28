"""Extract API endpoints (path + method + body/param fields) from the Rising Stones
web chunks. Pattern in the minified source is always:

    t({url:"".concat(BASE,"posts/like"),method:"post",data:o})
    t({url:"".concat(a,"glamour/favorite"),method:"post",data:{id:o,favorite_id:r}})
    t({url:BASE+"recruit/party",method:"get",params:o})

so we scan for url:...method:...(data|params) triples.
"""
import os, re, json, collections

root = 'C:/Users/Administrator/AppData/Local/Temp/rs_js'
main = 'C:/Users/Administrator/AppData/Local/Temp/rs_main.js'

# url:  "".concat(X,"path")  |  X+"path"  |  "literal"
# then method:"verb", then data:{...} / data:x / params:{...} / params:x
call = re.compile(
    r'url:\s*(?:"".concat\(\s*(\w+)\s*,\s*"([^"]*)"\s*\)|(\w+)\s*\+\s*"([^"]*)"|"([^"]*)")'
    r'\s*,\s*method:\s*"(\w+)"'
    r'(?:\s*,\s*(data|params):\s*(\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}|\w+))?',
    re.S,
)

found = collections.defaultdict(lambda: {'methods': set(), 'fields': set(), 'files': set(), 'base': set()})

files = [main] + [os.path.join(root, f) for f in sorted(os.listdir(root))]
for p in files:
    try:
        txt = open(p, encoding='utf-8', errors='replace').read()
    except Exception:
        continue
    name = os.path.basename(p)
    for m in call.finditer(txt):
        basevar = m.group(1) or m.group(3) or ''
        path = m.group(2) or m.group(4) or m.group(5) or ''
        method = m.group(6).upper()
        kind = m.group(7) or ''
        body = m.group(8) or ''
        if not path or path.startswith('http') or path.startswith('/pc'):
            continue
        # only keep things that look like api paths (a/b) or bare segment names
        if '/' not in path and not path.isalnum():
            continue
        rec = found[path]
        rec['methods'].add(method)
        rec['files'].add(name)
        rec['base'].add(basevar)
        if body.startswith('{'):
            for fm in re.finditer(r'([A-Za-z_]\w*)\s*:', body):
                rec['fields'].add(fm.group(1))
        elif body:
            rec['fields'].add('<%s:opaque>' % kind)
        if kind:
            rec['methods'].add('%s(%s)' % (method, kind))

print("%d endpoints\n" % len(found))
for path in sorted(found):
    r = found[path]
    methods = sorted(x for x in r['methods'] if '(' in x) or sorted(r['methods'])
    fields = sorted(f for f in r['fields'] if not f.startswith('<'))
    opaque = sorted(f for f in r['fields'] if f.startswith('<'))
    print("%-46s %-16s %s%s" % (
        path,
        ",".join(methods),
        ",".join(fields) if fields else "",
        (" " + ",".join(opaque)) if opaque else "",
    ))
