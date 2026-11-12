#!/usr/bin/env python3
"""One-time local bootstrap. Never writes credentials to disk or logs."""
import os, json, getpass, urllib.request, urllib.error
url=os.environ.get('SUPABASE_URL','').rstrip('/')
secret=os.environ.get('SUPABASE_SERVICE_ROLE_KEY','')
if url!='https://puewznmoypmlouxzzoqz.supabase.co': raise SystemExit('Refusing to use any project other than the dedicated GitHub Vault project.')
if not secret:
 raise SystemExit('Set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY in your shell, not in Git.')
if input(f'Create the sole admin sagedkhan746 on {url}? Type CREATE: ')!='CREATE': raise SystemExit('Cancelled')
pw=getpass.getpass('Admin password (12–128 characters): ')
if not 12<=len(pw)<=128 or pw!=getpass.getpass('Repeat password: '):raise SystemExit('Invalid password or mismatch')
def call(path,method,body=None):
 req=urllib.request.Request(url+path,data=None if body is None else json.dumps(body).encode(),method=method,headers={'apikey':secret,'Authorization':'Bearer '+secret,'Content-Type':'application/json','Prefer':'return=representation'})
 with urllib.request.urlopen(req,timeout=30) as res:
  raw=res.read(); return json.loads(raw) if raw else None
try:
 existing=call('/rest/v1/profiles?role=eq.ADMIN&select=id','GET')
 if existing:raise SystemExit('An admin already exists. No changes made.')
 user=call('/auth/v1/admin/users','POST',{'email':'sagedkhan746@users.vault.invalid','password':pw,'email_confirm':True})
 uid=user['id']
 try:
  call('/rest/v1/profiles','POST',{'id':uid,'username':'sagedkhan746','role':'ADMIN','status':'ACTIVE'})
 except Exception:
  call('/auth/v1/admin/users/'+uid,'DELETE');raise
 print('Admin created. Sign in through the Android app. No password was saved.')
except urllib.error.HTTPError as e:
 raise SystemExit(f'Bootstrap failed (HTTP {e.code}). Inspect project configuration; no secret is printed.')
