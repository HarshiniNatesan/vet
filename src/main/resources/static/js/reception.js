if (localStorage.getItem("role") !== "RECEPTIONIST") {
    location = "/login.html";
}

async function api(url, options = {}) {
    const r = await fetch(url, options);
    const text = await r.text();

    let d = {};
    try {
        d = text ? JSON.parse(text) : {};
    } catch (_) {
        d = { message: text };
    }

    if (!r.ok) {
        throw new Error(d.message || d.error || "Request failed");
    }
    return d;
}

const esc = v => String(v ?? "-")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

let vets = [];
let owners = [];

const el = id => document.getElementById(id);

function fillSelect(id, items, label, emptyText) {
    const select = el(id);
    select.innerHTML =
        `<option value="">${esc(emptyText)}</option>` +
        items.map(x =>
            `<option value="${esc(x.id)}">${esc(label(x))}</option>`
        ).join("");
}

async function init() {
    try {
        const [profile, vetData, ownerData] = await Promise.all([
            api("/api/reception/profile"),
            api("/api/reception/veterinarians"),
            api("/api/reception/owners")
        ]);

        vets = vetData;
        owners = ownerData;

        renderProfile(profile);

        fillSelect(
            "petVet",
            vets,
            v => `${v.fullName} - ${v.specialization || "Veterinarian"} (${v.availabilityStatus || "STATUS NOT SET"})`,
            "No veterinarian assignment"
        );

        fillSelect(
            "availabilityVet",
            vets,
            v => `${v.fullName} (ID: ${v.id})`,
            "Select veterinarian"
        );

        fillSelect(
            "operationVet",
            vets,
            v => `${v.fullName} (ID: ${v.id})`,
            "No veterinarian"
        );

        renderOwners();
        await loadPatientOptions();

        await Promise.all([
            loadPets(),
            loadVeterinarianAvailability(),
            loadWeek(),
            loadOperations(),
            loadMedicalRecords()
        ]);
    } catch (e) {
        console.error(e);
        el("receptionProfile").innerHTML =
            `<p>${esc(e.message)}</p>`;
    }
}


async function loadPatientOptions(query = "") {
    const list = el("patientOptions");
    if (!list) return;
    try {
        const data = await api("/api/reception/pets?search=" + encodeURIComponent(query));
        list.innerHTML = data.map(p =>
            `<option value="${esc(p.petCode)}">${esc(p.name)} — Owner: ${esc(p.owner?.fullName || "-")}</option>`
        ).join("");
    } catch (e) {
        list.innerHTML = "";
    }
}

function renderProfile(p) {
    el("receptionProfile").innerHTML = `
        <div class="item">
            <b>${esc(p.fullName)}</b><br>
            Role: ${esc(p.role)}<br>
            Employee ID: ${esc(p.id)}<br>
            Email: ${esc(p.email)}<br>
            Phone: ${esc(p.phone)}<br>
            Address: ${esc(p.address)}
        </div>
    `;
}

async function registerOwnerAndPet() {
    const body = {
        ownerName: el("ownerName").value.trim(),
        ownerAddress: el("ownerAddress").value.trim(),
        ownerPhone: el("ownerPhone").value.trim(),
        ownerEmail: el("ownerEmail").value.trim(),

        petName: el("petName").value.trim(),
        petSpecies: el("petSpecies").value.trim(),
        petBreed: el("petBreed").value.trim(),
        petGender: el("petGender").value.trim(),
        petAge: el("petAge").value,
        petWeight: el("petWeight").value,
        petColor: el("petColor").value.trim(),
        petAllergies: el("petAllergies").value.trim(),
        petConditions: el("petConditions").value.trim(),
        veterinarianId: el("petVet").value
    };

    el("registrationMsg").textContent = "";
    el("generatedCredentials").innerHTML = "";

    try {
        const result = await api("/api/reception/owners/register-with-pet", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });

        el("registrationMsg").textContent = result.savedToDatabase === false
            ? "Registration could not be confirmed."
            : "Pet owner and pet registered successfully and saved to the database.";

        el("generatedCredentials").innerHTML = `
            <div class="item">
                <b>Registration Successful</b><br>
                Owner ID: ${esc(result.owner.id)}<br>
                Pet ID: <b>${esc(result.pet.petCode)}</b><br>
                Pet Name: ${esc(result.pet.name)}<br>
                <br>
                <b>System-generated Pet Owner Login</b><br>
                Username: <b>${esc(result.generatedUsername)}</b><br>
                Initial Password: <b>${esc(result.generatedPassword)}</b><br>
                <small>Give these generated login details to the pet owner.</small>
            </div>
        `;

        clearRegistrationForm();

        [vets, owners] = await Promise.all([
            api("/api/reception/veterinarians"),
            api("/api/reception/owners")
        ]);

        fillSelect(
            "petVet",
            vets,
            v => `${v.fullName} - ${v.specialization || "Veterinarian"} (${v.availabilityStatus || "STATUS NOT SET"})`,
            "No veterinarian assignment"
        );

        fillSelect(
            "availabilityVet",
            vets,
            v => `${v.fullName} (ID: ${v.id})`,
            "Select veterinarian"
        );

        fillSelect(
            "operationVet",
            vets,
            v => `${v.fullName} (ID: ${v.id})`,
            "No veterinarian"
        );

        renderOwners();
        await loadPatientOptions();
        await loadPets();
    } catch (e) {
        el("registrationMsg").textContent = e.message;
    }
}

function clearRegistrationForm() {
    [
        "ownerName", "ownerAddress", "ownerPhone", "ownerEmail",
        "petName", "petSpecies", "petBreed", "petGender",
        "petAge", "petWeight", "petColor", "petAllergies",
        "petConditions"
    ].forEach(id => el(id).value = "");

    el("petVet").value = "";
}

async function loadVeterinarianAvailability() {
    try {
        const data = await api("/api/reception/veterinarians/availability");

        el("veterinarianAvailability").innerHTML = data.length
            ? data.map(v => `
                <div class="item">
                    <b>Dr. ${esc(v.fullName)}</b>
                    | Vet ID: ${esc(v.id)}<br>
                    Specialization: ${esc(v.specialization || "-")}<br>
                    Qualification: ${esc(v.qualification || "-")}<br>
                    Current Status:
                    <b>${esc(v.availabilityStatus || "STATUS NOT SET")}</b>
                    ${v.weekSchedule && v.weekSchedule.length
                        ? `<br><br><b>This Week:</b><br>` +
                          v.weekSchedule.map(s =>
                              `${esc(s.date)} - <b>${esc(s.status)}</b>`
                          ).join("<br>")
                        : "<br>This week's date-wise availability has not been entered yet."
                    }
                </div>
            `).join("")
            : "<p>No veterinarians registered.</p>";
    } catch (e) {
        el("veterinarianAvailability").innerHTML =
            `<p>${esc(e.message)}</p>`;
    }
}

async function saveAvailability() {
    try {
        await api("/api/reception/veterinarian-availability", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                veterinarianId: el("availabilityVet").value,
                date: el("availabilityDate").value,
                status: el("availabilityStatus").value,
            })
        });

        el("availabilityMsg").textContent = "Availability saved successfully.";
        await loadVeterinarianAvailability();
    } catch (e) {
        el("availabilityMsg").textContent = e.message;
    }
}

async function loadPets() {
    try {
        const search = el("petSearch").value || "";
        const data = await api(
            "/api/reception/pets?search=" + encodeURIComponent(search)
        );

        el("petsList").innerHTML = data.length
            ? data.map(p => `
                <div class="item">
                    <b>${esc(p.petCode)} - ${esc(p.name)}</b><br>
                    Species: ${esc(p.species)}
                    | Breed: ${esc(p.breed)}
                    | Gender: ${esc(p.gender)}
                    | Age: ${esc(p.age)}
                    | Weight: ${esc(p.weight)} kg<br>
                    Owner:
                    ${esc(p.owner?.fullName)}
                    (Owner ID: ${esc(p.owner?.id)})
                    | Phone: ${esc(p.owner?.phone)}<br>
                    Veterinarian:
                    ${esc(p.assignedVeterinarian?.fullName || "Not assigned")}
                </div>
            `).join("")
            : "<p>No registered pets found.</p>";
    } catch (e) {
        el("petsList").innerHTML =
            `<p>Unable to load registered pets: ${esc(e.message)}</p>`;
    }
}

function renderOwners() {
    el("ownersList").innerHTML = owners.length
        ? owners.map(o => `
            <div class="item">
                <b>${esc(o.fullName)}</b>
                | Owner ID: ${esc(o.id)}<br>
                Address: ${esc(o.address)}<br>
                Phone: ${esc(o.phone)}
                | Email: ${esc(o.email)}<br>
                Registered Pets:
                ${esc((o.pets || []).join(", ") || "None")}
            </div>
        `).join("")
        : "<p>No owners registered.</p>";
}

async function loadWeek() {
    const [a, g] = await Promise.all([
        api("/api/reception/appointments/week"),
        api("/api/reception/grooming/week")
    ]);

    el("weeklyAppointments").innerHTML = a.length
        ? a.map(x => `
            <div class="item">
                <b>#${esc(x.id)}</b>
                ${esc(x.petId)} - ${esc(x.petName)}
                | Owner: ${esc(x.owner)}
                | Dr: ${esc(x.veterinarian)}
                | ${esc(x.date)} ${esc(x.time)}
                | ${esc(x.status)}<br>
                ${esc(x.reason || "")}
            </div>
        `).join("")
        : "<p>No veterinarian appointments this week.</p>";

    el("weeklyGrooming").innerHTML = g.length
        ? g.map(x => `
            <div class="item">
                <b>#${esc(x.id)}</b>
                ${esc(x.petId)} - ${esc(x.petName)}
                | Owner: ${esc(x.owner)}
                | ${esc(x.service)}
                | ${esc(x.date)} ${esc(x.time)}
                | ${esc(x.status)}
            </div>
        `).join("")
        : "<p>No spa/grooming appointments this week.</p>";
}

async function loadOperations() {
    const data = await api("/api/reception/operations/week");

    el("operations").innerHTML = data.length
        ? data.map(x => `
            <div class="item">
                <b>#${esc(x.id)}</b>
                ${esc(x.petId)} - ${esc(x.petName)}
                | ${esc(x.operationType)}
                | ${esc(x.date)} ${esc(x.time)}
                | Dr: ${esc(x.veterinarian)}
                | ${esc(x.status)}
                | ${esc(x.notes || "")}
            </div>
        `).join("")
        : "<p>No operations scheduled this week.</p>";
}

async function createOperation() {
    try {
        await api("/api/reception/operations", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                petId: el("operationPet").value,
                veterinarianId: el("operationVet").value,
                operationType: el("operationType").value,
                operationDate: el("operationDate").value,
                operationTime: el("operationTime").value,
                status: el("operationStatus").value,
                notes: el("operationNotes").value
            })
        });

        el("operationMsg").textContent = "Operation scheduled.";
        await loadOperations();
    } catch (e) {
        el("operationMsg").textContent = e.message;
    }
}

async function loadMedicalRecords() {
    const data = await api("/api/reception/medical-records");

    el("medicalRecords").innerHTML = data.length
        ? data.map(r => `
            <div class="item">
                <b>${esc(r.petId)} - ${esc(r.petName)}</b>
                | ${esc(r.date)}
                | Dr. ${esc(r.veterinarian)}<br>
                Diagnosis: ${esc(r.diagnosis)}<br>
                Treatment: ${esc(r.treatment)}<br>
                Symptoms: ${esc(r.symptoms)}<br>
                Notes: ${esc(r.notes)}
            </div>
        `).join("")
        : "<p>No medical records available.</p>";
}

function logout() {
    localStorage.clear();
    location = "/login.html";
}

init();


const operationPatientInput = el("operationPet");
if (operationPatientInput) {
    operationPatientInput.addEventListener("input", () => loadPatientOptions(operationPatientInput.value.trim()));
    operationPatientInput.addEventListener("change", () => {
        const value = operationPatientInput.value.trim();
        if (value) loadPatientOptions(value);
    });
}
