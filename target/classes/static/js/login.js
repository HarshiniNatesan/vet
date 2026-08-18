async function login(){
 const username=document.getElementById("username").value,password=document.getElementById("password").value,role=document.getElementById("role").value;
 try{
  const r=await fetch("/api/auth/login",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({username,password})});
  const d=await r.json(); if(!r.ok) throw new Error(d.message||"Login failed");
  if(d.role!==role) throw new Error("Selected role does not match this account.");
  localStorage.setItem("username",username);localStorage.setItem("role",d.role);
  location.href=d.role==="PET_OWNER"?"/owner.html":d.role==="VETERINARIAN"?"/vet.html":d.role==="RECEPTIONIST"?"/reception.html":"/role.html?role="+d.role;
 }catch(e){document.getElementById("msg").textContent=e.message}
}