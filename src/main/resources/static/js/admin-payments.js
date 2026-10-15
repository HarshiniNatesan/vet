if(localStorage.getItem('role')!=='ADMIN')location='/login.html';
async function api(u){const r=await fetch(u);const t=await r.text();let d={};try{d=t?JSON.parse(t):{};}catch{d={message:t}}if(!r.ok)throw new Error(d.message||'Request failed');return d;}
const esc=v=>PaymentUI.esc(v==null||v===''?'-':v);
const typeLabel=t=>t==='DOCTOR_CONSULTATION'?'Consultation':t==='PHARMACY'?'Medicine':t;
const statusLabel=s=>PaymentUI.isPaid(s)?'PAID':s;
function showError(m){const el=document.getElementById('paymentsError');if(el)el.textContent=m||'';}
async function loadPayments(){try{showError('');const s=document.getElementById('status').value;const rows=await api('/api/admin/payments'+(s?'?status='+s:''));document.getElementById('payments').innerHTML=rows.map(p=>{const paid=PaymentUI.isPaid(p.status);const method=PaymentUI.methodText(p);return `<div class="item"><b>Invoice ${esc(p.invoiceNumber)}</b> — ${esc(typeLabel(p.paymentType))} — ₹${esc(p.amount)} — <b>${esc(statusLabel(p.status))}</b><br>Pet: ${esc(p.pet)} | Owner: ${esc(p.owner)} | Branch: ${esc(p.branch)}`+(paid?`<br>Paid at: ${esc(PaymentUI.fmtDateTime(p.paidAt))}${method?` | Method: ${esc(method)}`:''}${p.transactionId?` | Ref: ${esc(p.transactionId)}`:''}`:'')+`</div>`;}).join('')||'<p>No payment records.</p>';}catch(e){showError(e.message)}}
async function load(){try{showError('');const s=await api('/api/admin/payments/summary');document.getElementById('summary').innerHTML=`<div class="summary-card"><h3>Total</h3><p>${esc(s.total)}</p></div><div class="summary-card"><h3>Paid</h3><p>${esc(s.success)}</p></div><div class="summary-card"><h3>Pending</h3><p>${esc(s.pending)}</p></div><div class="summary-card"><h3>Revenue</h3><p>₹${esc(s.revenue)}</p></div>`;await loadPayments();}catch(e){showError(e.message)}}
load();
