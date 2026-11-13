const {chromium}=require('playwright');const fs=require('fs');const path=require('path');const root=path.resolve(__dirname,'../../..');
(async()=>{const b=await chromium.launch({args:['--no-sandbox']});const p=await b.newPage({viewport:{width:700,height:900},deviceScaleFactor:1});
for(const mode of ['repos-light','repos-dark','files-light','files-dark','error-light']){
 await p.goto('file://'+path.join(root,'docs/design/approved-reference.html'));
 if(mode.startsWith('files'))await p.locator('[data-act="repo"]').first().click();
 if(mode.startsWith('error'))await p.locator('#errorDemo').click();
 await p.evaluate((mode)=>{const phone=document.getElementById('phone');const symbols=document.querySelector('body > svg');document.body.replaceChildren(symbols,phone);Object.assign(document.body.style,{margin:'0',padding:'0',background:'white'});Object.assign(phone.style,{width:'360px',height:'640px',maxWidth:'none',border:'0',borderRadius:'0',margin:'0',boxShadow:'none'});phone.querySelector('.status').style.display='none';phone.dataset.reduce='true';if(mode.endsWith('dark'))phone.classList.add('dark');window.scrollTo(0,0);},mode);
 await p.waitForTimeout(300);
 fs.mkdirSync(path.join(root,'docs/design/html'),{recursive:true});
 await p.locator('#phone').screenshot({path:path.join(root,'docs/design/html',mode+'.png')});
}
await b.close();})().catch(e=>{console.error(e);process.exit(1)});
