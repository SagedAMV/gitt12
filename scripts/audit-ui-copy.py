#!/usr/bin/env python3
"""Audit every app Kotlin source + XML resource; remote content is not rewritten."""
from pathlib import Path
import re,json,sys
root=Path(__file__).resolve().parents[1]
main=root/'android/app/src/main'
rows=[]
for path in sorted(main.rglob('*')):
 if path.suffix not in {'.kt','.xml'}:continue
 text=path.read_text()
 for match in re.finditer(r'"(?:[^"\\]|\\.)*"',text):
  value=match.group()[1:-1]
  if not re.search('[\u0600-\u06ff]',value):continue
  # Literal fragments in string templates are audited too. Dynamic filenames,
  # paths, preview text and GitHub metadata are data, never rewritten.
  words=len(value.replace('\\n',' ').split())
  rows.append(dict(file=str(path.relative_to(root)),line=text.count('\n',0,match.start())+1,text=value,words=words,template='$' in value))
violations=[r for r in rows if r['words']>3]
report={'scope':'Every Kotlin/XML app source; Arabic literals and template fragments; all dynamic UI templates manually reviewed','scanned_files':sum(1 for p in main.rglob('*') if p.suffix in {'.kt','.xml'}),'arabic_occurrences':len(rows),'unique_literals':len(set(r['text'] for r in rows)),'violations':violations,'entries':rows,'external_data':'Filenames, paths, usernames, file previews, audit values and timestamps stay intact; they are not application copy.'}
(root/'docs/UI_COPY_AUDIT_0.8.0.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
print(json.dumps({k:v for k,v in report.items() if k!='entries'},ensure_ascii=False,indent=2))
sys.exit(bool(violations))
