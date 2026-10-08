async function loadStatistics(){
 loading("stats");
 try{
  const s=await api("/api/pharmacy/stats");
  const items=[["Total Medicines",s.totalMedicines],["Stock Units",s.totalStockUnits],["Low Stock Medicines",s.lowStockMedicines],["Dispensed Today",s.medicinesDispensedToday],["Pending Prescriptions",s.pendingPrescriptions],["Pharmacy Transactions",s.totalPharmacyTransactions]];
  $("stats").innerHTML=items.map(x=>`<div class="stat-card"><strong>${esc(x[1])}</strong><span>${esc(x[0])}</span></div>`).join("");
  const rows=s.fastMovingMedicines||[];
  $("fastBody").innerHTML=rows.length?rows.map(x=>`<tr><td>${esc(x.medicineName)}</td><td>${esc(x.medicineId)}</td><td>${esc(x.quantityDispensed)}</td><td>${esc(x.transactionCount)}</td><td>${esc(x.availableStock)}</td></tr>`).join(""):'<tr><td colspan="5" class="state">No dispensing transactions are available yet.</td></tr>';
 }catch(e){error("stats",e.message);$("fastBody").innerHTML=`<tr><td colspan="5">${esc(e.message)}</td></tr>`;}
}
loadStatistics();
