shellInit();
async function open(){
 const id=new URLSearchParams(location.search).get('id');if(!id)return;loading('dispensePanel');
 try{
  const p=await api('/api/pharmacy/prescriptions/'+id);$('dispensePanel').className='panel';
  $('dispensePanel').innerHTML=`<div class="detail-grid"><div><h3>Patient</h3><p><b>${esc(p.pet.name)}</b> (${esc(p.pet.petCode)})<br>${esc(p.pet.species)} · ${esc(p.pet.breed)}<br>Owner: ${esc(p.pet.owner?.fullName)}<br>Contact: ${esc(p.pet.owner?.phone)}</p></div><div><h3>Veterinarian</h3><p>${esc(p.veterinarian?.fullName)}<br>ID: ${esc(p.veterinarian?.id)}</p></div><div><h3>Prescription</h3><p>#${esc(p.id)}<br><b>${esc(p.medicineName)}</b><br>${esc(p.dosage)} · ${esc(p.frequency)} · ${esc(p.duration)}<br>Required: ${esc(p.requiredQuantity||1)}</p></div></div><div class="dispense-form"><label>Quantity to dispense<input id="dispQty" type="number" min="1" value="${esc(p.requiredQuantity||1)}"></label><button class="btn primary" id="dispenseBtn">Dispense medicine</button></div><div id="dispenseMsg" role="status" aria-live="polite"></div>`;
  $('dispenseBtn').onclick=async()=>{
   const btn=$('dispenseBtn');btn.disabled=true;
   try{
    const q=Number($('dispQty').value);if(!Number.isInteger(q)||q<=0)throw new Error('Enter a valid quantity greater than zero.');
    const t=await api('/api/pharmacy/dispense/'+p.id,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({quantity:q})});
    $('dispenseMsg').innerHTML=`<div class="success">Medicine dispensed successfully. Transaction #${esc(t.id)} was recorded.</div><button class="btn primary" id="pharmacyBillBtn">Generate medicine bill</button><div id="pharmacyBillError" class="error" role="status" aria-live="polite"></div><div id="pharmacyInvoice"></div>`;
    document.getElementById('pharmacyBillBtn').onclick=async()=>{
     const billBtn=document.getElementById('pharmacyBillBtn'),error=document.getElementById('pharmacyBillError'),invoice=document.getElementById('pharmacyInvoice');error.textContent='';billBtn.disabled=true;
     try{
      const bill=await api('/api/payments/staff/pharmacy/'+p.id,{method:'POST'});const paid=['CONFIRMED','SUCCESS','PAID'].includes(String(bill.status).toUpperCase());
      invoice.innerHTML=`<h3>Medicine Invoice</h3><table><thead><tr><th>Medicine</th><th>Quantity</th><th>Unit Price</th><th>Subtotal</th></tr></thead><tbody>${(bill.items||[]).map(i=>`<tr><td>${esc(i.medicineName)}</td><td>${esc(i.quantity)}</td><td>₹${Number(i.unitPrice||0).toFixed(2)}</td><td>₹${Number(i.subtotal||0).toFixed(2)}</td></tr>`).join('')}</tbody></table><div id="pharmacyPay" style="margin-top:12px"></div>`;
      billBtn.hidden=true;
      PaymentUI.mount(document.getElementById('pharmacyPay'),bill,api);
      window.dispatchEvent(new CustomEvent('payment-status-updated',{detail:{billId:bill.id,status:bill.status,amount:bill.amount}}));try{localStorage.setItem('pawsCarePaymentUpdated',JSON.stringify({billId:bill.id,status:bill.status,at:Date.now()}));}catch(_){}
     }catch(err){error.textContent=err.message||'Unable to generate the medicine bill.';billBtn.disabled=false;}
    };
   }catch(e){$('dispenseMsg').innerHTML=`<div class="error">${esc(e.message)}</div>`;btn.disabled=false;}
  };
 }catch(e){error('dispensePanel',e.message)}
}
open();
