const user = localStorage.getItem("username");

if (!user || localStorage.getItem("role") !== "VETERINARIAN") {
    location = "/login.html";
}

async function api(url, options = {}) {
    const response = await fetch(url, options);
    const text = await response.text();

    let data;
    try {
        data = text ? JSON.parse(text) : {};
    } catch (_) {
        data = { message: text };
    }

    if (!response.ok) {
        throw new Error(
            data.message || data.error || "Request failed"
        );
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

function petCode(pet) {
    return pet?.petCode ||
        (pet?.id
            ? `PET-${String(pet.id).padStart(6, "0")}`
            : "-");
}


async function loadVetProfile() {
    try {
        const d = await api(`/api/vet/profile/${encodeURIComponent(user)}`);
        document.getElementById("vetProfile").innerHTML = `
            <div class="item">
                <b>Doctor Name:</b> ${esc(d.fullName)}<br>
                <b>Doctor ID:</b> ${esc(d.id)}<br>
                <b>Username:</b> ${esc(d.username)}<br>
                <b>Specialization:</b> ${esc(d.specialization)}<br>
                <b>Qualification:</b> ${esc(d.qualification)}<br>
                <b>Phone:</b> ${esc(d.phone)}<br>
                <b>Availability:</b> ${esc(d.availabilityStatus)}
            </div>`;
    } catch(e) {
        document.getElementById("vetProfile").innerHTML = `<p>${esc(e.message)}</p>`;
    }
}

async function loadAppointments() {
    try {
        const appointments =
            await api(`/api/vet/appointments/${encodeURIComponent(user)}`);

        const container =
            document.getElementById("appointments");

        if (!appointments.length) {
            container.innerHTML =
                "<p>No appointments assigned to you.</p>";
            return;
        }

        container.innerHTML = appointments.map(a => `
            <div class="item">
                <h4>Appointment #${esc(a.id)}</h4>

                <p>
                    <b>Priority:</b>
                    ${esc(a.priority || "-")}
                </p>

                <p>
                    <b>Pet:</b>
                    ${esc(a.pet?.name || "Unknown")}
                    (${esc(petCode(a.pet))})
                </p>

                <p>
                    <b>Owner:</b>
                    ${esc(a.owner?.fullName || "Unknown")}
                </p>

                <p>
                    <b>Date:</b>
                    ${esc(a.appointmentDate)}
                </p>

                <p>
                    <b>Time:</b>
                    ${esc(a.appointmentTime)}
                </p>

                <p>
                    <b>Reason:</b>
                    ${esc(a.reason || "-")}
                </p>

                <p>
                    <b>Status:</b>
                    ${esc(a.status || "-")}
                </p>

                <button onclick="selectPatient(
                    '${esc(petCode(a.pet))}'
                )">
                    Open Patient
                </button>

                <button onclick="updateStatus(
                    ${a.id}, 'CONFIRMED'
                )">
                    Accept
                </button>

                <button onclick="updateStatus(
                    ${a.id}, 'IN_PROGRESS'
                )">
                    In Progress
                </button>

                <button onclick="updateStatus(
                    ${a.id}, 'COMPLETED'
                )">
                    Complete
                </button>

                <button onclick="updateStatus(
                    ${a.id}, 'REJECTED'
                )">
                    Reject
                </button>
            </div>
        `).join("");

    } catch (error) {
        console.error(error);
        document.getElementById("appointments").innerHTML =
            `<p>${esc(error.message)}</p>`;
    }
}

async function loadPatients() {
    try {
        const patients =
            await api(`/api/vet/patients/${encodeURIComponent(user)}`);

        document.getElementById("patients").innerHTML =
            patients.map(p => `
                <div class="item">
                    <b>${esc(petCode(p))}</b>
                    — ${esc(p.name)}

                    <br>
                    <button onclick="selectPatient(
                        '${esc(petCode(p))}'
                    )">
                        View Patient
                    </button>
                </div>
            `).join("") ||
            "<p>No allotted patients.</p>";

    } catch (e) {
        document.getElementById("patients").innerHTML =
            `<p>${esc(e.message)}</p>`;
    }
}

async function selectPatient(code) {
    document.getElementById("searchPetId").value = code;
    await openPatient(code);
}

async function searchPet() {
    const query = document.getElementById("searchPetId").value.trim();
    const message = document.getElementById("searchMessage");
    const results = document.getElementById("searchResults");

    if (!query) {
        message.textContent = "Enter a Pet ID, pet name or owner name.";
        if (results) results.innerHTML = "";
        return;
    }

    try {
        message.textContent = "Searching...";
        const matches = await api(
            `/api/vet/patient-search/${encodeURIComponent(user)}?q=${encodeURIComponent(query)}`
        );
        if (results) {
            results.innerHTML = matches.length
                ? matches.map(p => `
                    <div class="item">
                        <b>${esc(petCode(p))} — ${esc(p.name)}</b><br>
                        Owner: ${esc(p.owner?.fullName || "-")} | Species: ${esc(p.species || "-")} | Breed: ${esc(p.breed || "-")}
                        <br><button onclick="selectPatient('${esc(petCode(p))}')">Select Patient</button>
                    </div>`).join("")
                : "<p>No matching patients found.</p>";
        }
        message.textContent = matches.length === 1 ? "Patient found." : `${matches.length} patient(s) found.`;
        if (matches.length === 1) await openPatient(petCode(matches[0]));
    } catch (e) {
        message.textContent = e.message;
        if (results) results.innerHTML = "";
    }
}

async function openPatient(code) {
    const message = document.getElementById("searchMessage");
    try {
        const pet = await api(
            `/api/vet/patient-search/${encodeURIComponent(user)}/${encodeURIComponent(code)}`
        );
        message.textContent = "Patient selected.";
        document.getElementById("petId").value = petCode(pet);
        document.getElementById("vaccPetId").value = petCode(pet);
        document.getElementById("ppId").value = petCode(pet);
        document.getElementById("petDetails").innerHTML = `
            <div class="item">
                <b>${esc(pet.name)} — ${esc(petCode(pet))}</b><br>
                Species: ${esc(pet.species)}<br>
                Breed: ${esc(pet.breed)}<br>
                Gender: ${esc(pet.gender)}<br>
                Age: ${esc(pet.age)}<br>
                Weight: ${esc(pet.weight)} kg<br>
                Color: ${esc(pet.color)}<br>
                Allergies: ${esc(pet.allergies || "None")}<br>
                Medical Conditions: ${esc(pet.medicalConditions || "None")}<br>
                Owner: ${esc(pet.owner?.fullName || "-")}<br>
                Owner Contact: ${esc(pet.owner?.phone || "-")}
            </div>`;
        await loadPatientRecords(petCode(pet));
    } catch (e) {
        message.textContent = e.message;
    }
}

async function loadPatientRecords(code) {
    try {
        const [medical, vaccines, prescriptions] = await Promise.all([
            api(`/api/vet/reports/${encodeURIComponent(user)}/${encodeURIComponent(code)}`),
            api(`/api/vet/vaccinations/${encodeURIComponent(user)}/${encodeURIComponent(code)}`),
            api(`/api/vet/prescriptions/${encodeURIComponent(user)}/${encodeURIComponent(code)}`)
        ]);

        renderMedicalHistory(medical);
        renderVaccinations(vaccines);
        renderPrescriptions(prescriptions);

    } catch (e) {
        alert(e.message);
    }
}

function renderMedicalHistory(records) {
    document.getElementById("medicalHistory").innerHTML =
        records.map(r => `
            <div class="item">
                <b>Record #${esc(r.id)}</b><br>
                Date: ${esc(r.visitDate || "-")}<br>
                Diagnosis: ${esc(r.diagnosis || "-")}<br>
                Symptoms: ${esc(r.symptoms || "-")}<br>
                Treatment: ${esc(r.treatment || "-")}<br>
                Notes: ${esc(r.notes || "-")}<br>

                <button onclick="editMedicalRecord(${r.id})">
                    Update Medical Record
                </button>
            </div>
        `).join("") ||
        "<p>No medical records available.</p>";

    window.currentMedicalRecords = records;
}

function editMedicalRecord(id) {
    const r =
        (window.currentMedicalRecords || [])
        .find(x => Number(x.id) === Number(id));

    if (!r) return;

    document.getElementById("reportId").value = r.id;
    document.getElementById("visitDate").value =
        r.visitDate || "";
    document.getElementById("symptoms").value =
        r.symptoms || "";
    document.getElementById("observations").value =
        r.observations || "";
    document.getElementById("diagnosis").value =
        r.diagnosis || "";
    document.getElementById("treatment").value =
        r.treatment || "";
    document.getElementById("followUpDate").value =
        r.followUpDate || "";
    document.getElementById("surgery").checked =
        !!r.surgeryRequired;
    document.getElementById("surgeryDetails").value =
        r.surgeryDetails || "";
    document.getElementById("notes").value =
        r.notes || "";

    window.scrollTo({
        top: document.getElementById("reportId")
            .getBoundingClientRect().top +
            window.scrollY - 100,
        behavior: "smooth"
    });
}

async function saveMedicalRecord() {
    try {
        const id =
            document.getElementById("reportId").value;

        const body = {
            petId:
                document.getElementById("petId").value,

            visitDate:
                document.getElementById("visitDate").value,

            symptoms:
                document.getElementById("symptoms").value,

            observations:
                document.getElementById("observations").value,

            diagnosis:
                document.getElementById("diagnosis").value,

            treatment:
                document.getElementById("treatment").value,

            followUpDate:
                document.getElementById("followUpDate").value,

            surgeryRequired:
                document.getElementById("surgery").checked,

            surgeryDetails:
                document.getElementById("surgeryDetails").value,

            notes:
                document.getElementById("notes").value
        };

        if (id) {
            await api(
                `/api/vet/reports/${id}/${encodeURIComponent(user)}`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(body)
                }
            );

            alert("Medical record updated successfully.");
        } else {
            await api(
                `/api/vet/reports/${encodeURIComponent(user)}`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(body)
                }
            );

            alert("Medical record saved successfully.");
        }

        const code =
            document.getElementById("petId").value;

        clearMedicalForm();
        await loadPatientRecords(code);

    } catch (e) {
        alert(e.message);
    }
}

function clearMedicalForm() {
    document.getElementById("reportId").value = "";
    document.getElementById("visitDate").value = "";
    document.getElementById("symptoms").value = "";
    document.getElementById("observations").value = "";
    document.getElementById("diagnosis").value = "";
    document.getElementById("treatment").value = "";
    document.getElementById("followUpDate").value = "";
    document.getElementById("surgery").checked = false;
    document.getElementById("surgeryDetails").value = "";
    document.getElementById("notes").value = "";
}

function renderVaccinations(records) {
    document.getElementById("vaccinationList").innerHTML =
        records.map(v => `
            <div class="item">
                <b>${esc(v.vaccineName || "-")}</b>
                — ${esc(v.status || "PENDING")}<br>

                Type:
                ${esc(v.vaccineType || "-")}<br>

                Date Given:
                ${esc(v.administeredDate || "-")}<br>

                Next Due:
                ${esc(v.nextDueDate || "-")}<br>

                Veterinarian:
                ${esc(v.veterinarian?.fullName || "-")}<br>

                Notes:
                ${esc(v.notes || "-")}<br>

                <button onclick="editVaccination(${v.id})">
                    Update Vaccination
                </button>
            </div>
        `).join("") ||
        "<p>No vaccination history available.</p>";

    window.currentVaccinations = records;
}

function editVaccination(id) {
    const v =
        (window.currentVaccinations || [])
        .find(x => Number(x.id) === Number(id));

    if (!v) return;

    document.getElementById("vaccinationId").value =
        v.id;

    document.getElementById("vaccPetId").value =
        petCode(v.pet);

    document.getElementById("vaccineName").value =
        v.vaccineName || "";

    document.getElementById("vaccineType").value =
        v.vaccineType || "";

    document.getElementById("administeredDate").value =
        v.administeredDate || "";

    document.getElementById("nextDueDate").value =
        v.nextDueDate || "";

    document.getElementById("vaccineStatus").value =
        v.status || "PENDING";

    document.getElementById("vaccineNotes").value =
        v.notes || "";
}

async function saveVaccination() {
    try {
        const id =
            document.getElementById("vaccinationId").value;

        const body = {
            petId:
                document.getElementById("vaccPetId").value,

            vaccineName:
                document.getElementById("vaccineName").value,

            vaccineType:
                document.getElementById("vaccineType").value,

            administeredDate:
                document.getElementById("administeredDate").value,

            nextDueDate:
                document.getElementById("nextDueDate").value,

            status:
                document.getElementById("vaccineStatus").value,

            notes:
                document.getElementById("vaccineNotes").value
        };

        if (id) {
            await api(
                `/api/vet/vaccinations/${id}/${encodeURIComponent(user)}`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(body)
                }
            );

            alert("Vaccination updated successfully.");
        } else {
            await api(
                `/api/vet/vaccinations/${encodeURIComponent(user)}`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(body)
                }
            );

            alert("Vaccination saved successfully.");
        }

        const code =
            document.getElementById("vaccPetId").value;

        clearVaccinationForm();
        await loadPatientRecords(code);

    } catch (e) {
        alert(e.message);
    }
}

function clearVaccinationForm() {
    document.getElementById("vaccinationId").value = "";
    document.getElementById("vaccineName").value = "";
    document.getElementById("vaccineType").value = "";
    document.getElementById("administeredDate").value = "";
    document.getElementById("nextDueDate").value = "";
    document.getElementById("vaccineStatus").value = "PENDING";
    document.getElementById("vaccineNotes").value = "";
}

function renderPrescriptions(records) {
    document.getElementById("prescriptionList").innerHTML =
        records.map((p, index) => `
            <div class="item">
                ${
                    index === 0
                    ? "<b>Latest Prescription</b><br>"
                    : ""
                }

                Prescription #${esc(p.id)}<br>
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


async function prescribe() {
    try {
        const code =
            document.getElementById("ppId").value;

        await api(
            `/api/vet/prescriptions/${encodeURIComponent(user)}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    petId: code,
                    medicineName:
                        document.getElementById("medicine").value,
                    dosage:
                        document.getElementById("dosage").value,
                    frequency:
                        document.getElementById("frequency").value,
                    duration:
                        document.getElementById("duration").value,
                    instructions:
                        document.getElementById("instructions").value
                })
            }
        );

        alert("Prescription saved successfully.");

        document.getElementById("medicine").value = "";
        document.getElementById("dosage").value = "";
        document.getElementById("frequency").value = "";
        document.getElementById("duration").value = "";
        document.getElementById("instructions").value = "";

        await loadPatientRecords(code);

    } catch (e) {
        alert(e.message);
    }
}

async function updateStatus(id, status) {
    try {
        await api(
            `/api/vet/appointments/${id}/status`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ status })
            }
        );

        await loadAppointments();

    } catch (e) {
        alert(
            "Unable to update appointment: " +
            e.message
        );
    }
}

function logout() {
    localStorage.clear();
    location = "/login.html";
}

loadVetProfile();
loadAppointments();
loadPatients();
