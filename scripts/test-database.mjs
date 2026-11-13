import { PGlite } from '@electric-sql/pglite';
import { readFile, readdir } from 'node:fs/promises';
const db = new PGlite();
await db.exec(`
 create role anon; create role authenticated; create role service_role bypassrls;
 create schema auth;
 create table auth.users(id uuid primary key);
 create function auth.uid() returns uuid language sql stable as $$ select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid $$;
 grant usage on schema auth to authenticated;
 grant execute on function auth.uid() to authenticated;
`);
for (const f of (await readdir('supabase/migrations')).sort()) await db.exec(await readFile('supabase/migrations/'+f,'utf8'));
const ids={admin:'11111111-1111-4111-8111-111111111111',user:'22222222-2222-4222-8222-222222222222',other:'33333333-3333-4333-8333-333333333333',pending:'44444444-4444-4444-8444-444444444444',disabled:'55555555-5555-4555-8555-555555555555',temporary:'66666666-6666-4666-8666-666666666666'};
const repo='77777777-7777-4777-8777-777777777777';
for(const [name,id] of Object.entries(ids)) {
 await db.query('insert into auth.users values($1)',[id]);
 await db.query('insert into profiles(id,username,role,status,must_change_password) values($1,$2,$3,$4,$5)',[id,name==='admin'?'sagedkhan746':name,name==='admin'?'ADMIN':'USER',name==='pending'?'PENDING':name==='disabled'?'DISABLED':'ACTIVE',name==='temporary']);
}
await db.query("insert into repositories(id,github_id,owner,name) values($1,1,'owner','repo')",[repo]);
for(const name of ['user','pending','disabled','temporary']) await db.query('insert into user_repository_access values($1,$2)',[ids[name],repo]);
await db.query('insert into registration_requests(user_id) values($1)',[ids.pending]);
await db.query("insert into audit_logs(actor_id,action,outcome) values($1,'test','success')",[ids.admin]);
let passed=0;
async function as(name,fn) {
 await db.exec('begin; set local role authenticated');
 await db.query("select set_config('request.jwt.claim.sub',$1,true)",[ids[name]]);
 try {await fn();} finally {await db.exec('rollback')}
}
function assert(condition,message){if(!condition)throw new Error(message);passed++;console.log('PASS',message)}
for(const [name,count] of [['admin',1],['user',1],['other',0],['pending',0],['disabled',0],['temporary',0]]) {
 await as(name,async()=>assert((await db.query('select * from repositories')).rows.length===count,`${name}: repository isolation`));
}
await as('user',async()=>{assert((await db.query('select * from audit_logs')).rows.length===0,'User cannot read audit');assert((await db.query('select * from profiles')).rows.length===1,'User cannot list others');});
await as('admin',async()=>assert((await db.query('select * from audit_logs')).rows.length===1,'Admin reads audit'));
for(const sql of ["update profiles set role='ADMIN'",'delete from repositories',"insert into audit_logs(action,outcome) values('forged','success')",'select * from rate_limits']) {
 let rejected=false;try{await as('user',()=>db.exec(sql));}catch{rejected=true}assert(rejected,`Client denied: ${sql}`);
}
let rejected=false;
try {await db.query("update profiles set role='ADMIN' where id=$1",[ids.other]);}catch{rejected=true}
assert(rejected,'Second Admin rejected');
await db.query('select review_registration($1,$2,true)',[ids.pending,ids.admin]);
assert((await db.query('select status from profiles where id=$1',[ids.pending])).rows[0].status==='ACTIVE','Review activates pending account');
assert((await db.query('select status from registration_requests where user_id=$1',[ids.pending])).rows[0].status==='APPROVED','Review updates request atomically');
rejected=false;try{await db.query('select review_registration($1,$2,false)',[ids.pending,ids.admin]);}catch{rejected=true}assert(rejected,'Duplicate review rejected');
for(let i=0;i<10;i++)assert((await db.query("select consume_attempt('key') as ok")).rows[0].ok,'Rate limit allows attempt '+(i+1));
assert(!(await db.query("select consume_attempt('key') as ok")).rows[0].ok,'Rate limit rejects attempt 11');
await db.close();console.log(`Database tests: ${passed} passed`);
