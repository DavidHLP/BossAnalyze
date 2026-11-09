const {chromium, expect} = require('/home/david/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test');
const fs=require('node:fs'), path=require('node:path'), assert=require('node:assert/strict');
if (!process.argv[2]) throw new Error('Usage: node tools/workflow-fixture/frontend.cjs <frontend-dto.json>');
process.umask(0o077);
const dir=fs.mkdtempSync('/tmp/boss-workflow-frontend-'), origin='http://127.0.0.1:5020';
const input=path.resolve(process.argv[2]);
console.log(JSON.stringify({output:dir}));
const dto=JSON.parse(fs.readFileSync(input,'utf8'));
assert.equal(dto.synthetic,true,'Only the controlled workflow synthetic DTO is accepted');
fs.copyFileSync(input,path.join(dir,'original-http-dto.json'));
const jobs=dto.items.map(({job,analysisStatus,model,pipelineVersion})=>({...job,analysisStatus,analysisModel:model,analysisVersion:pipelineVersion}));
const urls={city:origin+'/api/v1/boss/basic/city-name-list',position:origin+'/api/v1/boss/basic/position-name-list?cityName=all',salary:origin+'/api/v1/boss/user/salary-hot-job?limit=10',jobs:origin+'/api/v1/boss/user/two-dimensional-analysis-chart?cityName=all&positionName=all&xAxis=salary_value&yAxis=degree_value'};
fs.writeFileSync(path.join(dir,'adapted-jobs.json'),JSON.stringify(jobs,null,2));
const results={input,origin,identity:'fixture session only; no real login/logout',adapter:{job:'items[].job spread unchanged',analysisStatus:'items[].analysisStatus',analysisModel:'items[].model',analysisVersion:'items[].pipelineVersion',city:'unique items[].job.cityName',position:'unique items[].job.positionName',salary:'[] (explicit empty ranking fixture; not match scores)'},checks:[],requests:[],errors:[],ownedProcesses:[]};
const write=()=>fs.writeFileSync(path.join(dir,'result.json'),JSON.stringify(results,null,2));
const check=(name)=>results.checks.push(name);
function owned(profile){return fs.readdirSync('/proc').filter(x=>/^\d+$/.test(x)).flatMap(pid=>{try{const c=fs.readFileSync(`/proc/${pid}/cmdline`,'utf8');if(c.includes(profile))return [{pid:Number(pid),starttime:fs.readFileSync(`/proc/${pid}/stat`,'utf8').split(') ')[1].split(' ')[19]}]}catch{}return []})}
(async()=>{
 const profile=fs.mkdtempSync('/tmp/boss-frontend-profile-'); fs.chmodSync(profile,0o700);
 let context;
 try {
 context=await chromium.launchPersistentContext(profile,{executablePath:'/usr/bin/chromium',headless:false,viewport:{width:1440,height:1100},serviceWorkers:'block',args:['--no-first-run','--disable-background-networking']});
 results.ownedProcesses=owned(profile);
 await context.addInitScript(()=>{localStorage.setItem('token','synthetic-ui-fixture-only');localStorage.setItem('token_timestamp',String(Date.now()))});
 let empty=false;
 await context.route('**/*',async route=>{
  const r=route.request(), u=r.url(); let data,action='abort';
  if(r.method()==='GET'){
   if(u===origin+'/api/auth/getUserBaseInfo')data={name:'合成展示 QA',email:'fixture-ui@example.invalid'};
   else if([origin+'/api/auth/getUserPrivateInformation',origin+'/api/auth/getRouters'].includes(u))data=[];
   else if(u===urls.city)data=[...new Set(jobs.map(j=>j.cityName))];
   else if(u===urls.position)data=[...new Set(jobs.map(j=>j.positionName))];
   else if(u===urls.salary)data=[];
   else if(u===urls.jobs)data=empty?[]:jobs;
   if(data!==undefined)action='fixture';
   else {const p=new URL(u);if(p.origin===origin && (p.pathname==='/recruitment'||p.pathname.startsWith('/_nuxt/')||['/favicon.ico','/favicon.svg'].includes(p.pathname)))action='local-static'}
  }
  results.requests.push({method:r.method(),url:u,action});
  if(action==='fixture')await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(data)});
  else if(action==='local-static')await route.continue();
  else await route.abort('blockedbyclient');
 });
 const page=context.pages()[0]; page.on('pageerror',e=>results.errors.push(e.message));
 await page.goto(origin+'/recruitment');
 await expect(page.getByRole('heading',{name:'招聘数据分析',exact:true})).toBeVisible();
 await expect(page.getByText('选择条件，开始查询',{exact:true})).toBeVisible(); check('existing idle state');
 await expect(page.getByRole('button',{name:'查询分析',exact:true})).toBeEnabled();
 await page.getByRole('button',{name:'查询分析',exact:true}).click();
 const rows=page.locator('tr').filter({has:page.getByRole('button',{name:/^查看.*详情$/})});
 await expect(rows).toHaveCount(jobs.length);
 assert.deepEqual(await rows.locator('td:first-child button').allTextContents(),jobs.map(j=>j.positionName));check('three DTO rows in original order; salary and city visible');
 for(let i=0;i<jobs.length;i++){
  const j=jobs[i]; await expect(rows.nth(i)).toContainText(j.salary);await expect(rows.nth(i)).toContainText(j.cityName);
  await rows.nth(i).getByRole('button',{name:/^查看.*详情$/}).click();
  const dialog=page.getByRole('dialog');await expect(dialog).toBeVisible();
  await expect(dialog.locator('dt').filter({hasText:/^职位 ID$/}).locator('..').locator('dd')).toHaveText(j.positionId);
  const req=dialog.locator('section').filter({has:page.getByRole('heading',{name:'工作要求',exact:true})});
  const ben=dialog.locator('section').filter({has:page.getByRole('heading',{name:'职位福利',exact:true})});
  assert.deepEqual(await req.locator('li').allTextContents(),j.jobRequirements);
  assert.deepEqual(await ben.locator('[data-slot="badge"]').allTextContents(),j.employeeBenefits);
  await expect(dialog.getByLabel('分析来源')).toContainText(j.analysisStatus==='complete'?'已完成（当前来源）':'等待分析');
  await expect(dialog.getByLabel('分析来源')).toContainText(`分析来源：${j.analysisModel} · ${j.analysisVersion}`);
  const link=dialog.getByRole('link',{name:'职位来源',exact:true}); await expect(link).toHaveAttribute('href',j.jobUrl); await expect(link).toHaveAttribute('target','_blank'); await expect(link).toHaveAttribute('rel','noopener noreferrer'); assert.equal(await dialog.getByRole('link').count(),1); results.links??=[];results.links.push({jobId:j.positionId,href:await link.getAttribute('href'),target:await link.getAttribute('target'),rel:await link.getAttribute('rel')});
  await expect(dialog.locator('dt').filter({hasText:/^地址$/}).locator('..').locator('dd')).toHaveText('—');
  if(!j.jobRequirements.length){await expect(req).toContainText('暂无可用工作要求。');await expect(ben).toContainText('暂无可用福利信息。');await expect(dialog).toContainText('以下保留已有内容，未经当前来源的成功分析确认。');}
  check(`job ${j.positionId}: exact Unicode requirements/benefits arrays, status/model/version, nonempty source link safe attributes, null address shown as em dash`);
  if(i===0){await dialog.getByRole('heading',{name:'工作要求',exact:true}).scrollIntoViewIfNeeded();await page.screenshot({path:path.join(dir,'detail.png')});}
  await dialog.getByRole('button',{name:'关闭',exact:true}).first().click();await expect(dialog).toHaveCount(0);
 }
 empty=true;await page.getByRole('button',{name:'查询分析',exact:true}).click();
 await expect(page.getByText('暂无匹配的职位样本',{exact:true})).toBeVisible();await expect(rows).toHaveCount(0);check('explicit [] response uses existing no-result state; no stale rows');
 assert(results.requests.some(r=>r.url===urls.jobs));assert.equal(results.errors.length,0);assert(results.requests.filter(r=>r.action==='abort').every(r=>r.url==='https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;600;700&display=swap'));check('exact fixture APIs and local assets only; external Google font blocked; no JS errors');
 results.status='verified';
 }catch(e){results.status='failed';results.failure=e.message;process.exitCode=1}
 finally{if(context)await context.close();results.remainingOwnedProcesses=owned(profile);fs.rmSync(profile,{recursive:true,force:true});write();console.log(JSON.stringify({status:results.status,checks:results.checks.length,failure:results.failure,remainingOwnedProcesses:results.remainingOwnedProcesses}));}
})();
