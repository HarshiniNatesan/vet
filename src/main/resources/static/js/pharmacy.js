const username = localStorage.getItem("username");
const role = localStorage.getItem("role");

if (!username || role !== "PHARMACY") {
    location = "/login.html";
}

document.getElementById("who").textContent = username || "";

async function api(url, options = {}) {
    const response = await fetch(url, options);
    const text = await response.text();
    let data = {};
    try { data = text ? JSON.parse(text) : {}; }
    catch { data = { message: text }; }
    if (!response.ok) throw new Error(data.message || data.error || "Request failed");
    return data;
}

const esc = value => String(value ?? "-")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

const money = value => "₹" + Number(value || 0).toFixed(2);

function badge(status) {
    const safe = String(status ?? "UNKNOWN");
    return `<span class="badge ${safe.toLowerCase()}">${esc(safe.replaceAll("_", " "))}</span>`;
}

async function load() {
    try {
        await Promise.all([
            loadStats(),
            loadMedicines(),
            loadAlerts(),
            loadRecords(),
            loadProfile()
        ]);
    } catch (error) {
        console.error(error);
    }
}

async function loadStats() {
    const stats = await api("/api/pharmacy/stats");
    const items = [
        ["Total Medicines", stats.totalMedicines],
        ["Stock Units", stats.totalStockUnits],
        ["Low Stock", stats.lowStockMedicines],
        ["Dispensed Today", stats.medicinesDispensedToday],
        ["Pending Prescriptions", stats.pendingPrescriptions],
        ["Pharmacy Transactions", stats.totalPharmacyTransactions]
    ];
    document.getElementById("stats").innerHTML = items
        .map(item => `<div class="stat"><strong>${esc(item[1])}</strong><span>${esc(item[0])}</span></div>`)
        .join("");
}

async function loadMedicines() {
    const q = document.getElementById("medQ").value.trim();
    const data = await api("/api/pharmacy/medicines" + (q ? `?q=${encodeURIComponent(q)}` : ""));
    document.getElementById("medBody").innerHTML = data.map(medicine => `
        <tr>
            <td>${esc(medicine.id)}</td>
            <td><b>${esc(medicine.name)}</b><br><small>${esc(medicine.category)}</small></td>
            <td>${esc(medicine.batchNumber)}</td>
            <td>${esc(medicine.expiryDate)}</td>
            <td>${esc(medicine.quantity)}</td>
            <td>${esc(medicine.reorderLevel)}</td>
            <td>${money(medicine.unitPrice)}</td>
            <td>${badge(medicine.status)}</td>
            <td>
                <button class="small" onclick='editMedicine(${JSON.stringify(medicine)})'>Edit</button>
                <button class="small danger" onclick="deactivateMedicine(${medicine.id})">Deactivate</button>
            </td>
        </tr>
    `).join("") || `<tr><td colspan="9">No medicines found.</td></tr>`;
}

async function loadAlerts() {
    const data = await api("/api/pharmacy/low-stock");
    document.getElementById("alertsList").innerHTML = data.map(medicine => `
        <div class="alert-card">
            ${badge(medicine.status)}
            <h3>${esc(medicine.name)}</h3>
            <p>Current stock: <b>${esc(medicine.quantity)}</b> · Reorder level: <b>${esc(medicine.reorderLevel)}</b></p>
            <p>Batch: ${esc(medicine.batchNumber)} · Expiry: ${esc(medicine.expiryDate)}</p>
        </div>
    `).join("") || `<div class="empty-state">No low-stock medicines.</div>`;
}

function patientCard(pet, containerId) {
    const prescriptions = pet.prescriptions || [];
    document.getElementById(containerId).innerHTML = `
        <div class="patient-card">
            <div class="patient-header">
                <div>
                    <h3>${esc(pet.name)} <span class="muted">(${esc(pet.petCode)})</span></h3>
                    <p class="muted">Patient record and veterinarian prescriptions</p>
                </div>
            </div>
            <div class="patient-meta">
                <div class="meta-box"><small>Species</small><b>${esc(pet.species)}</b></div>
                <div class="meta-box"><small>Breed</small><b>${esc(pet.breed)}</b></div>
                <div class="meta-box"><small>Age</small><b>${esc(pet.age)}</b></div>
                <div class="meta-box"><small>Owner</small><b>${esc(pet.owner?.fullName)}</b></div>
                <div class="meta-box"><small>Owner Contact</small><b>${esc(pet.owner?.phone)}</b></div>
                <div class="meta-box"><small>Veterinarian</small><b>${esc(pet.prescriptions?.[0]?.veterinarian?.fullName || "Assigned veterinarian")}</b></div>
            </div>
            <h3 style="margin-top:22px">Prescribed Medicines</h3>
            ${prescriptions.length ? prescriptions.map(prescription => `
                <div class="prescription-card">
                    <div class="prescription-top">
                        <div><b>Prescription #${esc(prescription.id)}</b><br><span>${esc(prescription.medicineName)}</span></div>
                        <div>${badge(prescription.status)}</div>
                    </div>
                    <div class="prescription-details">
                        <div><small>Dosage</small><br><b>${esc(prescription.dosage)}</b></div>
                        <div><small>Frequency</small><br><b>${esc(prescription.frequency)}</b></div>
                        <div><small>Duration</small><br><b>${esc(prescription.duration)}</b></div>
                        <div><small>Required Quantity</small><br><b>${esc(prescription.requiredQuantity || 1)}</b></div>
                        <div><small>Veterinarian</small><br><b>${esc(prescription.veterinarian?.fullName)}</b></div>
                    </div>
                    <p><small>Instructions:</small> ${esc(prescription.instructions)}</p>
                    <button class="small" onclick="openDispense(${prescription.id})">Open for Dispensing</button>
                </div>
            `).join("") : `<div class="empty-state">No prescriptions found for this patient.</div>`}
        </div>
    `;
}

async function searchPatient(targetId = "patientResults", queryElementId = "patientQ") {
    const q = document.getElementById(queryElementId).value.trim();
    if (!q) {
        document.getElementById(targetId).innerHTML = `<div class="empty-state">Enter a Pet ID, pet name or owner name.</div>`;
        return;
    }

    try {
        const matches = await api("/api/pharmacy/patients/search?q=" + encodeURIComponent(q));
        if (!matches.length) {
            document.getElementById(targetId).innerHTML = `<div class="empty-state">No matching patient found.</div>`;
            return;
        }

        document.getElementById(targetId).innerHTML = matches.map(pet => `
            <div class="list-item">
                <div>
                    <b>${esc(pet.petCode)} · ${esc(pet.name)}</b><br>
                    <small>${esc(pet.species)} · Owner: ${esc(pet.owner?.fullName)}</small>
                </div>
                <button class="small" onclick="showPatient('${esc(pet.petCode)}', '${targetId}')">View Prescriptions</button>
            </div>
        `).join("");
    } catch (error) {
        document.getElementById(targetId).innerHTML = `<div class="error">${esc(error.message)}</div>`;
    }
}

async function showPatient(code, targetId = "patientResults") {
    try {
        const patient = await api("/api/pharmacy/patients/" + encodeURIComponent(code));
        patientCard(patient, targetId);
    } catch (error) {
        document.getElementById(targetId).innerHTML = `<div class="error">${esc(error.message)}</div>`;
    }
}

async function globalSearch() {
    const q = document.getElementById("globalQ").value.trim();
    if (!q) {
        document.getElementById("globalResults").innerHTML = `<div class="empty-state">Enter a Pet ID, pet name or owner name.</div>`;
        return;
    }

    try {
        const matches = await api("/api/pharmacy/patients/search?q=" + encodeURIComponent(q));
        if (!matches.length) {
            document.getElementById("globalResults").innerHTML = `<div class="empty-state">No matching patient found.</div>`;
            return;
        }
        document.getElementById("globalResults").innerHTML = matches.map(pet => `
            <div class="list-item">
                <div><b>${esc(pet.petCode)} · ${esc(pet.name)}</b><br><small>Owner: ${esc(pet.owner?.fullName)}</small></div>
                <button class="small" onclick="showPatient('${esc(pet.petCode)}','globalResults')">View Prescriptions</button>
            </div>
        `).join("");
    } catch (error) {
        document.getElementById("globalResults").innerHTML = `<div class="error">${esc(error.message)}</div>`;
    }
}

async function openDispense(id) {
    try {
        const prescription = await api("/api/pharmacy/prescriptions/" + id);
        location.hash = "dispensing";
        document.getElementById("dispensePanel").classList.remove("empty");
        document.getElementById("dispensePanel").innerHTML = `
            <div class="detail-grid">
                <div>
                    <h3>Patient</h3>
                    <p><b>${esc(prescription.pet.name)}</b> (${esc(prescription.pet.petCode)})<br>
                    ${esc(prescription.pet.species)} · ${esc(prescription.pet.breed)}<br>
                    Owner: ${esc(prescription.pet.owner?.fullName)}<br>
                    Contact: ${esc(prescription.pet.owner?.phone)}</p>
                </div>
                <div>
                    <h3>Veterinarian</h3>
                    <p>${esc(prescription.veterinarian?.fullName)}<br>ID: ${esc(prescription.veterinarian?.id)}</p>
                </div>
                <div>
                    <h3>Prescription</h3>
                    <p>#${esc(prescription.id)}<br>${esc(prescription.medicineName)}<br>
                    ${esc(prescription.dosage)} · ${esc(prescription.frequency)} · ${esc(prescription.duration)}<br>
                    Required: ${esc(prescription.requiredQuantity || 1)}<br>${esc(prescription.instructions)}</p>
                </div>
            </div>
            <div class="dispense-form">
                <label>Quantity to dispense<input id="dispQty" type="number" min="1" value="${esc(prescription.requiredQuantity || 1)}"></label>
                <button onclick="dispense(${prescription.id})">Dispense Medicine</button>
            </div>
            <div id="dispenseMsg"></div>
        `;
    } catch (error) {
        document.getElementById("dispensePanel").innerHTML = `<div class="error">${esc(error.message)}</div>`;
    }
}

async function dispense(id) {
    try {
        const quantity = Number(document.getElementById("dispQty").value);
        if (!Number.isInteger(quantity) || quantity <= 0) throw new Error("Enter a valid quantity greater than zero.");
        const transaction = await api(`/api/pharmacy/dispense/${id}?username=${encodeURIComponent(username)}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ quantity })
        });
        document.getElementById("dispenseMsg").innerHTML = `<div class="success">Medicine dispensed successfully. Transaction #${esc(transaction.id)} was recorded.</div>`;
        await load();
    } catch (error) {
        document.getElementById("dispenseMsg").innerHTML = `<div class="error">${esc(error.message)}</div>`;
    }
}

async function loadRecords() {
    const data = await api("/api/pharmacy/records");
    document.getElementById("recordBody").innerHTML = data.map(transaction => `
        <tr>
            <td>#${esc(transaction.id)}</td>
            <td>#${esc(transaction.prescriptionId)}</td>
            <td>${esc(transaction.pet?.petCode)}<br>${esc(transaction.pet?.name)}</td>
            <td>${esc(transaction.medicineName)}</td>
            <td>${esc(transaction.quantityDispensed)}</td>
            <td>${esc(transaction.veterinarian?.fullName)}</td>
            <td>${esc(transaction.pharmacyStaff?.fullName)}</td>
            <td>${esc(transaction.dispensingDate)}</td>
            <td>${money(transaction.totalAmount)}</td>
        </tr>
    `).join("") || `<tr><td colspan="9">No dispensing transactions yet.</td></tr>`;
}

function openMedicine(medicine = {}) {
    document.getElementById("modal").classList.remove("hidden");
    document.getElementById("modalTitle").textContent = medicine.id ? "Edit Medicine" : "Add Medicine";
    const fields = [
        ["mid", medicine.id], ["mn", medicine.name], ["mc", medicine.category],
        ["mm", medicine.manufacturer], ["mb", medicine.batchNumber], ["me", medicine.expiryDate],
        ["mq", medicine.quantity], ["mr", medicine.reorderLevel], ["mp", medicine.unitPrice], ["md", medicine.description]
    ];
    fields.forEach(([id, value]) => document.getElementById(id).value = value ?? "");
}

function editMedicine(medicine) { openMedicine(medicine); }
function closeMedicine() { document.getElementById("modal").classList.add("hidden"); }

async function deactivateMedicine(id) {
    if (!confirm("Deactivate this medicine? Existing pharmacy records will remain unchanged.")) return;
    try {
        await api(`/api/pharmacy/medicines/${id}`, { method: "DELETE" });
        await Promise.all([loadStats(), loadMedicines(), loadAlerts()]);
    } catch (error) {
        alert(error.message);
    }
}

document.getElementById("medForm").onsubmit = async event => {
    event.preventDefault();
    const id = document.getElementById("mid").value;
    const body = {
        name: document.getElementById("mn").value,
        category: document.getElementById("mc").value,
        manufacturer: document.getElementById("mm").value,
        batchNumber: document.getElementById("mb").value,
        expiryDate: document.getElementById("me").value,
        quantity: document.getElementById("mq").value,
        reorderLevel: document.getElementById("mr").value,
        unitPrice: document.getElementById("mp").value,
        description: document.getElementById("md").value
    };

    try {
        await api("/api/pharmacy/medicines" + (id ? "/" + id : ""), {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });
        closeMedicine();
        await Promise.all([loadStats(), loadMedicines(), loadAlerts()]);
    } catch (error) {
        alert(error.message);
    }
};

async function loadProfile() {
    document.getElementById("profileBox").innerHTML = `
        <p><b>Username:</b> ${esc(username)}</p>
        <p><b>Role:</b> Pharmacy Staff</p>
        <p>Pharmacy staff can manage medicine inventory, review veterinarian prescriptions, dispense medicines and view pharmacy transactions.</p>
    `;
}

async function logout() {
    try { await api("/api/auth/logout", { method: "POST" }); }
    finally { localStorage.clear(); location = "/login.html"; }
}

load();
