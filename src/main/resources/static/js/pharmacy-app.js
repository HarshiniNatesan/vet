const username = localStorage.getItem('username');
const role = localStorage.getItem('role');
if (!username || role !== 'PHARMACY') location = '/login.html';
const $ = id => document.getElementById(id);
async function api(url, options={}){const r=await fetch(url,options);const t=await r.text();let d={};try{d=t?JSON.parse(t):{}}catch{d={message:t}}if(!r.ok)throw new Error(d.message||d.error||'Request failed');return d}
const esc=v=>String(v??'-').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&#039;');
const money=v=>'₹'+Number(v||0).toFixed(2);
const badge=s=>{s=String(s??'UNKNOWN');return `<span class="badge ${s.toLowerCase()}">${esc(s.replaceAll('_',' '))}</span>`};
function setUser(){const w=$('who');if(w)w.textContent=username||'Pharmacy Staff'}
function nav(){document.querySelectorAll('[data-nav]').forEach(a=>{if(location.pathname.endsWith(a.dataset.nav)||(!location.pathname.split('/').pop()&&a.dataset.nav==='pharmacy.html'))a.classList.add('active')})}
async function logout(){try{await api('/api/auth/logout',{method:'POST'})}finally{localStorage.clear();location='/login.html'}}
function loading(id,msg='Loading…'){const e=$(id);if(e)e.innerHTML=`<div class="state loading">${msg}</div>`}
function error(id,msg){const e=$(id);if(e)e.innerHTML=`<div class="state error">${esc(msg)}</div>`}
function shellInit(){setUser();nav();document.querySelectorAll('[data-logout]').forEach(b=>b.onclick=logout)}

function formatDateTime(value){
 if(!value) return '-';
 const raw=String(value);
 const m=raw.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/);
 if(m){
  let h=Number(m[4]); const ap=h>=12?'PM':'AM'; h=h%12||12;
  return `${m[3]}-${m[2]}-${m[1]} ${String(h).padStart(2,'0')}:${m[5]} ${ap}`;
 }
 const d=new Date(value);
 if(Number.isNaN(d.getTime())) return raw;
 return d.toLocaleString('en-IN',{day:'2-digit',month:'2-digit',year:'numeric',hour:'2-digit',minute:'2-digit',hour12:true});
}
