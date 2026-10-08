const user = localStorage.getItem("username");
const role = localStorage.getItem("role");

if (!user || role !== "PET_OWNER") {
    location = "/login.html";
}

async function api(url, opt = {}) {
    const r = await fetch(url, opt);
    const text = await r.text();

    let data;
    try {
        data = text ? JSON.parse(text) : {};
    } catch (_) {
        data = { message: text };
    }

    if (!r.ok) {
        throw new Error(data.message || data.error || "Request failed");
    }

    return data;
}

function esc(value) {
    return String(value ?? "-")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function petCode(p) {
    return p && p.petCode
        ? p.petCode
        : (p && p.id ? `PET-${String(p.id).padStart(6, "0")}` : "-");
}

function setMinimumDates() {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, "0");
    const dd = String(today.getDate()).padStart(2, "0");
    const minDate = `${yyyy}-${mm}-${dd}`;

    document.getElementById("date").min = minDate;
    document.getElementById("gdate").min = minDate;
}

function isPastDateTime(dateValue, timeValue) {
    if (!dateValue || !timeValue) return false;

    const selected = new Date(`${dateValue}T${timeValue}`);
    return selected.getTime() < Date.now();
}

async function load() {
    try {
        setMinimumDates();

        const ps = await api(
            `/api/owner/pets/${encodeURIComponent(user)}`
        );

        renderPets(ps);

        const vs = await api("/api/owner/veterinarians");

        renderVeterinarians(vs);

        await loadAppointments();
        await loadGrooming();

        if (ps.length) {
            document.getElementById("pet").value = String(ps[0].id);
            document.getElementById("gpet").value = String(ps[0].id);
            await loadPetInfo(ps[0].id);
        } else {
            document.getElementById("selectedPet").innerHTML =
                "<p>No registered pets available.</p>";
        }

    } catch (e) {
        console.error(e);
        alert(e.message);
    }
}

function renderPets(ps) {
    const html = ps.map(p => `
        <div class="item">
            <b>${esc(p.name)} — ${esc(petCode(p))}</b>
            <br>Species: ${esc(p.species)}
            | Breed: ${esc(p.breed)}
            <br>Gender: ${esc(p.gender)}
            | Age: ${esc(p.age)}
            | Weight: ${esc(p.weight)} kg
            <br>Color: ${esc(p.color)}
            | Allergies: ${esc(p.allergies || "None")}
            <br>Medical Conditions:
            ${esc(p.medicalConditions || "None")}
            <br>
            <button onclick="loadPetInfo(${p.id})">View Pet Records</button>
            <button onclick="generatePetQR(${p.id})">Generate QR</button>
            <button onclick="downloadPetReport(${p.id})">Download Report</button>
        </div>
    `).join("");

    document.getElementById("pets").innerHTML =
        html || "<p>No pets registered.</p>";

    const options = ps.map(p =>
        `<option value="${esc(p.id)}">
            ${esc(p.name)} — ${esc(petCode(p))}
        </option>`
    ).join("");

    document.getElementById("pet").innerHTML = options || `<option value="">No pets available</option>`;
    document.getElementById("gpet").innerHTML = options || `<option value="">No pets available</option>`;
}

function renderVeterinarians(vs) {
    const doctors = document.getElementById("doctors");

    doctors.innerHTML = vs.map(v => {
        const status = v.availabilityStatus || "AVAILABLE";

        return `
            <div class="item">
                <b>${esc(v.fullName)}</b><br>
                Doctor ID: ${esc(v.id)}<br>
                Specialization:
                ${esc(v.specialization || "General Veterinary Care")}<br>
                Qualification:
                ${esc(v.qualification || "Not specified")}<br>
                Contact: ${esc(v.phone || "Not specified")}<br>
                Availability:
                <b>${esc(status)}</b>
            </div>
        `;
    }).join("") || "<p>No veterinarians registered.</p>";

    document.getElementById("vet").innerHTML = vs.map(v => {
        const status = (v.availabilityStatus || "AVAILABLE").toUpperCase();

        const unavailable =
            status === "ABSENT" ||
            status === "UNAVAILABLE" ||
            status === "NOT AVAILABLE" ||
            status === "LEAVE";

        return `
            <option value="${esc(v.id)}"
                    ${unavailable ? "disabled" : ""}>
                ${esc(v.fullName)}
                — ${esc(v.specialization || "Veterinarian")}
                ${unavailable ? " (Unavailable)" : ""}
            </option>
        `;
    }).join("");

    if (!vs.length) {
        document.getElementById("vet").innerHTML =
            "<option value=''>No veterinarians available</option>";
    }
}

async function loadAppointments() {
    const as = await api(
        `/api/owner/appointments/${encodeURIComponent(user)}`
    );

    document.getElementById("appointments").innerHTML =
        as.map(a => `
            <div class="item">
                <b>Appointment #${esc(a.id)}</b>
                — ${esc(a.status)}<br>

                Pet:
                ${esc(a.pet?.name)}
                (${esc(petCode(a.pet))})<br>

                Doctor:
                ${esc(a.veterinarian?.fullName || "Not assigned")}<br>

                Specialization:
                ${esc(a.veterinarian?.specialization || "Not specified")}<br>

                Date:
                ${esc(a.appointmentDate)}
                |
                Time:
                ${esc(a.appointmentTime)}<br>

                Reason:
                ${esc(a.reason || "-")}<br>
                <button onclick="location.href='/owner-payments.html?appointmentId=${encodeURIComponent(a.id)}'">Pay Consultation</button>

                ${
                    a.status !== "COMPLETED" &&
                    a.status !== "CANCELLED"
                    ? `<button class="danger-button"
                        onclick="cancelAppointment(${a.id})">
                        Cancel Appointment
                       </button>`
                    : ""
                }
            </div>
        `).join("") || "<p>No veterinary appointments.</p>";
}

async function loadGrooming() {
    const gs = await api(
        `/api/owner/grooming/${encodeURIComponent(user)}`
    );

    document.getElementById("grooming").innerHTML =
        gs.map(g => `
            <div class="item">
                <b>Grooming #${esc(g.id)}</b>
                — ${esc(g.status)}<br>

                Pet:
                ${esc(g.pet?.name)}
                (${esc(petCode(g.pet))})<br>

                Service:
                ${esc(g.serviceName)}<br>

                Date:
                ${esc(g.bookingDate)}
                |
                Time:
                ${esc(g.bookingTime)}<br>

                ${
                    g.status !== "COMPLETED" &&
                    g.status !== "CANCELLED"
                    ? `<button class="danger-button"
                        onclick="cancelGrooming(${g.id})">
                        Cancel Grooming
                       </button>`
                    : ""
                }
            </div>
        `).join("") || "<p>No spa & grooming appointments.</p>";
}

async function loadPetInfo(id) {
    try {
        const ps = await api(
            `/api/owner/pets/${encodeURIComponent(user)}`
        );

        const pet = ps.find(
            x => Number(x.id) === Number(id)
        );

        if (!pet) return;

        document.getElementById("selectedPet").innerHTML = `
            <div class="item">
                <b>${esc(pet.name)} — ${esc(petCode(pet))}</b><br>
                Species: ${esc(pet.species)}
                | Breed: ${esc(pet.breed)}<br>
                Gender: ${esc(pet.gender)}
                | Age: ${esc(pet.age)}
                | Weight: ${esc(pet.weight)} kg<br>
                Color: ${esc(pet.color)}<br>
                Allergies: ${esc(pet.allergies || "None")}<br>
                Existing Medical Conditions:
                ${esc(pet.medicalConditions || "None")}<br>
                Owner:
                ${esc(pet.owner?.fullName || user)}
                |
                Contact:
                ${esc(pet.owner?.phone || "-")}
            </div>
        `;

        const [rs, vs, psx] = await Promise.all([
            api(`/api/owner/reports/${encodeURIComponent(user)}/${id}`),
            api(`/api/owner/vaccinations/${encodeURIComponent(user)}/${id}`),
            api(`/api/owner/prescriptions/${encodeURIComponent(user)}/${id}`)
        ]);

        renderMedicalRecords(rs);
        renderVaccinations(vs);
        renderPrescriptions(psx);

    } catch (e) {
        alert(e.message);
    }
}

function renderMedicalRecords(records) {
    document.getElementById("reports").innerHTML =
        records.map(r => `
            <div class="item">
                <b>Visit:
                    ${esc(r.visitDate || "-")}
                </b><br>

                Doctor:
                ${esc(r.veterinarian?.fullName || "-")}<br>

                Symptoms:
                ${esc(r.symptoms || "-")}<br>

                Diagnosis:
                ${esc(r.diagnosis || "-")}<br>

                Treatment:
                ${esc(r.treatment || "-")}<br>

                Follow-up:
                ${esc(r.followUpDate || "-")}<br>

                Notes:
                ${esc(r.notes || "-")}
            </div>
        `).join("") ||
        "<p>No medical records available.</p>";
}

function renderVaccinations(records) {
    if (!records.length) {
        document.getElementById("vaccines").innerHTML =
            "<p>No vaccination history available.</p>";
        return;
    }

    document.getElementById("vaccines").innerHTML =
        records.map(v => {
            const status =
                (v.status || "PENDING").toUpperCase();

            return `
                <div class="item">
                    <b>${esc(v.vaccineName || "Unnamed vaccine")}</b>
                    — <b>${esc(status)}</b><br>

                    Type:
                    ${esc(v.vaccineType || "-")}<br>

                    Date Given:
                    ${esc(v.administeredDate || "-")}<br>

                    Next Due:
                    ${esc(v.nextDueDate || "-")}<br>

                    Veterinarian:
                    ${esc(v.veterinarian?.fullName || "-")}<br>

                    Notes:
                    ${esc(v.notes || "-")}
                </div>
            `;
        }).join("");
}

function renderPrescriptions(records) {
    document.getElementById("prescriptions").innerHTML =
        records.map((p, index) => `
            <div class="item">
                ${
                    index === 0
                    ? "<b>Latest Prescription</b><br>"
                    : ""
                }

                Prescription #${esc(p.id)}<br>
                Date:
                ${esc(p.createdDate || "-")}<br>

                Medicine:
                ${esc(p.medicineName || "-")}<br>

                Dosage:
                ${esc(p.dosage || "-")}<br>

                Frequency:
                ${esc(p.frequency || "-")}<br>

                Duration:
                ${esc(p.duration || "-")}<br>

                Instructions:
                ${esc(p.instructions || "-")}<br>

                Veterinarian:
                ${esc(p.veterinarian?.fullName || "-")}
            </div>
        `).join("") ||
        "<p>No prescription history available.</p>";
}


async function bookAppointment() {
    const petValue = document.getElementById("pet").value;
    const vetValue = document.getElementById("vet").value;
    const dateValue = document.getElementById("date").value;
    const timeValue = document.getElementById("time").value;
    const reasonValue = document.getElementById("reason").value;

    const message = document.getElementById("appointmentMessage");

    if (!petValue || !vetValue || !dateValue || !timeValue) {
        message.textContent =
            "Please select pet, veterinarian, date and time.";
        return;
    }

    if (isPastDateTime(dateValue, timeValue)) {
        message.textContent =
            "Appointment date and time cannot be in the past.";
        return;
    }

    try {
        await api(
            `/api/owner/appointments/${encodeURIComponent(user)}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    petId: petValue,
                    vetId: vetValue,
                    date: dateValue,
                    time: timeValue,
                    reason: reasonValue
                })
            }
        );

        message.textContent =
            "Veterinary appointment booked successfully.";

        document.getElementById("reason").value = "";

        await loadAppointments();

    } catch (e) {
        message.textContent = e.message;
    }
}

async function cancelAppointment(id) {
    if (!confirm("Cancel this veterinary appointment?")) return;

    try {
        await api(
            `/api/owner/appointments/${id}/cancel?username=${encodeURIComponent(user)}`,
            { method: "PUT" }
        );

        alert("Appointment cancelled.");
        await loadAppointments();

    } catch (e) {
        alert(e.message);
    }
}

async function bookGrooming() {
    const petValue = document.getElementById("gpet").value;
    const serviceValue = document.getElementById("service").value;
    const dateValue = document.getElementById("gdate").value;
    const timeValue = document.getElementById("gtime").value;

    const message = document.getElementById("groomingMessage");

    if (!petValue || !dateValue || !timeValue) {
        message.textContent =
            "Please select pet, date and time.";
        return;
    }

    if (isPastDateTime(dateValue, timeValue)) {
        message.textContent =
            "Spa & grooming date and time cannot be in the past.";
        return;
    }

    try {
        await api(
            `/api/owner/grooming/${encodeURIComponent(user)}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    petId: petValue,
                    serviceName: serviceValue,
                    date: dateValue,
                    time: timeValue
                })
            }
        );

        message.textContent =
            "Spa & grooming appointment booked successfully.";

        await loadGrooming();

    } catch (e) {
        message.textContent = e.message;
    }
}

async function cancelGrooming(id) {
    if (!confirm("Cancel this spa & grooming appointment?")) return;

    try {
        await api(
            `/api/owner/grooming/${id}/cancel?username=${encodeURIComponent(user)}`,
            { method: "PUT" }
        );

        alert("Grooming appointment cancelled.");
        await loadGrooming();

    } catch (e) {
        alert(e.message);
    }
}

document.getElementById("pet").addEventListener(
    "change",
    e => {
        document.getElementById("gpet").value = e.target.value;
        loadPetInfo(e.target.value);
    }
);

document.getElementById("gpet").addEventListener(
    "change",
    e => loadPetInfo(e.target.value)
);

document.getElementById("date").addEventListener(
    "change",
    function () {
        const today =
            new Date().toISOString().split("T")[0];

        if (this.value < today) {
            this.value = today;
        }
    }
);

document.getElementById("gdate").addEventListener(
    "change",
    function () {
        const today =
            new Date().toISOString().split("T")[0];

        if (this.value < today) {
            this.value = today;
        }
    }
);

async function generatePetQR(id) {
    try {
        const data = await api(`/api/pet-reports/pets/${id}/qr`, {method: "POST"});
        const w = window.open("", "_blank");
        if (!w) { alert("Please allow pop-ups to view the QR code."); return; }
        w.document.write(`<html><head><title>Pet Report QR</title></head><body style="font-family:Arial;text-align:center"><h2>${esc(data.petCode)}</h2><p>Scan to open this pet's secure report.</p><img src="${data.imageDataUrl}" style="max-width:420px"><p>Expires: ${esc(data.expiresAt)}</p></body></html>`);
        w.document.close();
    } catch(e) { alert(e.message); }
}

async function downloadPetReport(id) {
    try {
        const r = await fetch(`/api/pet-reports/pets/${id}/pdf`);
        if(!r.ok){const t=await r.text();throw new Error(t||"Report download failed");}
        const blob=await r.blob(); const url=URL.createObjectURL(blob); const a=document.createElement("a"); a.href=url; a.download=`pet-report-${id}.pdf`; a.click(); URL.revokeObjectURL(url);
    } catch(e) { alert(e.message); }
}

async function logout() {
    try {
        await api("/api/auth/logout", {
            method: "POST"
        });
    } catch (_) {}

    localStorage.clear();
    location = "/login.html";
}

load();
