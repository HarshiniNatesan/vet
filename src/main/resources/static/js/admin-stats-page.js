document.write('<script src="/js/admin-page-common.js"><\/script>');
async function loadStatistics(){
 try{
  const [s,r]=await Promise.all([api("/api/admin/stats"),api("/api/admin/records")]);
  const a=[
   ["Pet Owners / Customers",s.totalPetOwners],["Pets / Patients",s.totalPets],
   ["Veterinarians",s.totalVeterinarians],["Receptionists / Workers",s.totalReceptionists],
   ["Appointments",s.totalAppointments],["Prescriptions",s.totalPrescriptions],
   ["Medicines",r.medicines],["Medical Reports",r.medicalReports],
   ["Pharmacy Transactions",r.pharmacyTransactions],["Completed Appointments",s.completedAppointments],
   ["Pending Appointments",s.pendingAppointments],["Low Stock Medicines",s.lowStockMedicines],
   ["Vaccination Records",s.totalVaccinations], ["Grooming Bookings",s.totalGroomingBookings]
  ];
  document.getElementById("stats").innerHTML=a.map(x=>`<div class="summary-card"><h3>${esc(x[0])}</h3><strong>${x[1]??0}</strong></div>`).join("");
  document.getElementById("species").innerHTML=Object.entries(s.petsBySpecies||{}).map(([k,v])=>`<div class="bar-row"><span>${esc(k)}</span><b>${v}</b></div>`).join("")||"No pet data available.";
 }catch(e){document.getElementById("stats").innerHTML=`<div class="content-card"><p>${esc(e.message)}</p></div>`;}
}
setTimeout(loadStatistics,0);
