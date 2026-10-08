package com.vet.controller;

import com.vet.model.*;
import com.vet.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vet")
public class VetController {

    private final UserRepository users;
    private final AppointmentRepository appointments;
    private final PetRepository pets;
    private final MedicalReportRepository reports;
    private final PrescriptionRepository prescriptions;
    private final VaccinationRepository vaccinations;

    public VetController(UserRepository u,
                         AppointmentRepository a,
                         PetRepository p,
                         MedicalReportRepository r,
                         PrescriptionRepository pr,
                         VaccinationRepository v) {
        users=u;
        appointments=a;
        pets=p;
        reports=r;
        prescriptions=pr;
        vaccinations=v;
    }

    private User vet(String username) {
        org.springframework.security.core.Authentication current = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(current==null || !current.isAuthenticated() || !current.getName().equals(username))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can access only your own veterinarian account.");
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Veterinarian not found"));
    }

    private Pet assignedPet(User doctor, Long petId) {
        Pet pet=pets.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Pet not found"));

        boolean assigned = (pet.getAssignedVeterinarian()!=null &&
                pet.getAssignedVeterinarian().getId().equals(doctor.getId())) ||
                appointments.findDistinctPetsByVeterinarian(doctor).stream()
                .anyMatch(p -> p.getId().equals(pet.getId()));

        if(!assigned)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This pet is not assigned to you.");

        return pet;
    }

    /*
     * All appointments belonging to the logged-in veterinarian.
     */
    @GetMapping("/appointments/{username}")
    public List<Appointment> appointments(@PathVariable String username) {
        return appointments.findByVeterinarianOrderByAppointmentDateAscAppointmentTimeAsc(
                vet(username));
    }

    @GetMapping("/profile/{username}")
    public Map<String,Object> profile(@PathVariable String username) {
        User doctor=vet(username);
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",doctor.getId());
        m.put("username",doctor.getUsername());
        m.put("fullName",doctor.getFullName());
        m.put("specialization",doctor.getSpecialization());
        m.put("qualification",doctor.getQualification());
        m.put("email",doctor.getEmail());
        m.put("phone",doctor.getPhone());
        m.put("availabilityStatus",doctor.getAvailabilityStatus());
        return m;
    }


    @PutMapping("/profile/{username}")
    public Map<String,Object> updateProfile(@PathVariable String username,@RequestBody Map<String,String> body) {
        User doctor=vet(username);
        if(body.containsKey("fullName") && !body.get("fullName").trim().isEmpty()) doctor.setFullName(body.get("fullName").trim());
        if(body.containsKey("email")) doctor.setEmail(body.get("email"));
        if(body.containsKey("phone")) doctor.setPhone(body.get("phone"));
        users.save(doctor);
        return profile(username);
    }

    @GetMapping("/patients/{username}")
    public List<Pet> patients(@PathVariable String username) {
        User doctor=vet(username);
        Map<Long,Pet> result=new LinkedHashMap<>();
        pets.findByAssignedVeterinarian(doctor).forEach(p -> result.put(p.getId(),p));
        appointments.findDistinctPetsByVeterinarian(doctor).forEach(p -> result.put(p.getId(),p));
        return new ArrayList<>(result.values());
    }

    @GetMapping("/patient-search/{username}")
    public List<Pet> searchPatients(@PathVariable String username, @RequestParam String q) {
        User doctor=vet(username);
        String query=q==null?"":q.trim().toLowerCase();
        if(query.isEmpty()) return Collections.emptyList();
        Map<Long,Pet> result=new LinkedHashMap<>();
        for(Pet p:patients(username)) {
            String code=p.getPetCode()==null?"":p.getPetCode().toLowerCase();
            String name=p.getName()==null?"":p.getName().toLowerCase();
            String owner=p.getOwner()==null||p.getOwner().getFullName()==null?"":p.getOwner().getFullName().toLowerCase();
            if(code.contains(query)||name.contains(query)||owner.contains(query)||String.valueOf(p.getId()).equals(query)) result.put(p.getId(),p);
        }
        return new ArrayList<>(result.values());
    }

    @GetMapping("/patient-search/{username}/{petCode}")
    public Pet searchPatient(@PathVariable String username,
                             @PathVariable String petCode) {

        User doctor=vet(username);

        String code=petCode.trim().toUpperCase();
        if(!code.matches("PET-\\d{6}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid Pet ID. Use format PET-000001.");

        long id;
        try {
            id=Long.parseLong(code.substring(4));
        } catch(Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Pet ID.");
        }

        return assignedPet(doctor,id);
    }

    @GetMapping("/stats/{username}")
    public Map<String,Object> stats(@PathVariable String username) {
        User doctor=vet(username);
        Map<String,Object> x=new LinkedHashMap<>();
        x.put("patients", patients(username).size());
        x.put("appointments", appointments.findByVeterinarian(doctor).size());
        x.put("completedAppointments", appointments.findByVeterinarian(doctor).stream().filter(a->a.getStatus()==AppointmentStatus.COMPLETED).count());
        x.put("pendingAppointments", appointments.findByVeterinarian(doctor).stream().filter(a->a.getStatus()==AppointmentStatus.PENDING).count());
        x.put("prescriptions", prescriptions.findByVeterinarian(doctor).size());
        x.put("medicalReports", reports.findByVeterinarian(doctor).size());
        x.put("vaccinations", vaccinations.findByVeterinarian(doctor).size());
        return x;
    }

    @GetMapping("/all-priority-appointments")
    public List<Appointment> all() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User doctor = vet(username);
        List<Appointment> result = new ArrayList<>();
        for(Appointment a: appointments.findAllPrioritySorted()) {
            if(a.getVeterinarian()!=null && a.getVeterinarian().getId().equals(doctor.getId())) result.add(a);
            else if(doctor.getBranch()!=null && a.getBranch()!=null && doctor.getBranch().getId().equals(a.getBranch().getId())) result.add(a);
        }
        return result;
    }

    @PutMapping("/appointments/{id}/status")
    public Appointment status(@PathVariable Long id,
                              @RequestBody Map<String, String> body) {

        Appointment appointment=appointments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Appointment not found"));
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User doctor = vet(username);
        boolean allowed = appointment.getVeterinarian()!=null && appointment.getVeterinarian().getId().equals(doctor.getId());
        if(!allowed && doctor.getBranch()!=null && appointment.getBranch()!=null) allowed = doctor.getBranch().getId().equals(appointment.getBranch().getId());
        if(!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot modify an appointment outside your access scope.");

        String newStatus=body.get("status");

        try {
            appointment.setStatus(AppointmentStatus.valueOf(newStatus));
        } catch(Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid appointment status");
        }

        return appointments.save(appointment);
    }

    @GetMapping("/pets/{id}")
    public Pet pet(@PathVariable Long id) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return assignedPet(vet(username), id);
    }

    @GetMapping("/reports/{username}")
    public List<MedicalReport> reports(@PathVariable String username) {
        return reports.findByVeterinarian(vet(username));
    }

    @GetMapping("/reports/{username}/{petCode}")
    public List<MedicalReport> petReports(@PathVariable String username,
                                          @PathVariable String petCode) {
        User doctor=vet(username);
        Pet pet=petFromCode(petCode);
        assignedPet(doctor,pet.getId());
        return reports.findByPetAndCompletedTrue(pet);
    }

    @PostMapping("/reports/{username}")
    public MedicalReport createReport(@PathVariable String username,
                                      @RequestBody Map<String, String> body) {

        User doctor=vet(username);
        Pet pet=petFromBody(doctor,body);

        MedicalReport report=new MedicalReport();
        report.setVeterinarian(doctor);
        report.setBranch(doctor.getBranch());
        report.setPet(pet);
        report.setVisitDate(parseDateOrToday(body.get("visitDate")));
        report.setSymptoms(body.get("symptoms"));
        report.setObservations(body.get("observations"));
        report.setDiagnosis(body.get("diagnosis"));
        report.setTreatment(body.get("treatment"));
        report.setFollowUpDate(body.get("followUpDate"));
        report.setSurgeryRequired(Boolean.parseBoolean(
                body.getOrDefault("surgeryRequired","false")));
        report.setSurgeryDetails(body.get("surgeryDetails"));
        report.setNotes(body.get("notes"));
        report.setCompleted(true);

        return reports.save(report);
    }

    @PutMapping("/reports/{id}/{username}")
    public MedicalReport updateReport(@PathVariable Long id,
                                      @PathVariable String username,
                                      @RequestBody Map<String,String> body) {

        User doctor=vet(username);

        MedicalReport report=reports.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Medical record not found"));

        // A veterinarian may update records for any pet currently assigned to them,
        // including older records originally created by another veterinarian.
        assignedPet(doctor,report.getPet().getId());

        if(body.containsKey("visitDate") && !body.get("visitDate").trim().isEmpty())
            report.setVisitDate(parseDateOrToday(body.get("visitDate")));

        if(body.containsKey("symptoms")) report.setSymptoms(body.get("symptoms"));
        if(body.containsKey("observations")) report.setObservations(body.get("observations"));
        if(body.containsKey("diagnosis")) report.setDiagnosis(body.get("diagnosis"));
        if(body.containsKey("treatment")) report.setTreatment(body.get("treatment"));
        if(body.containsKey("followUpDate")) report.setFollowUpDate(body.get("followUpDate"));
        if(body.containsKey("surgeryRequired"))
            report.setSurgeryRequired(Boolean.parseBoolean(body.get("surgeryRequired")));
        if(body.containsKey("surgeryDetails")) report.setSurgeryDetails(body.get("surgeryDetails"));
        if(body.containsKey("notes")) report.setNotes(body.get("notes"));

        return reports.save(report);
    }

    @GetMapping("/vaccinations/{username}/{petCode}")
    public List<VaccinationRecord> vaccinations(@PathVariable String username,
                                                @PathVariable String petCode) {
        User doctor=vet(username);
        Pet pet=petFromCode(petCode);
        assignedPet(doctor,pet.getId());
        return vaccinations.findByPet(pet);
    }

    @PostMapping("/vaccinations/{username}")
    public VaccinationRecord saveVaccination(@PathVariable String username,
                                             @RequestBody Map<String,String> body) {

        User doctor=vet(username);
        Pet pet=petFromBody(doctor,body);

        VaccinationRecord v=new VaccinationRecord();
        v.setPet(pet);
        v.setVeterinarian(doctor);
        v.setVaccineName(required(body,"vaccineName"));
        v.setVaccineType(body.get("vaccineType"));
        v.setAdministeredDate(parseNullableDate(body.get("administeredDate")));
        v.setNextDueDate(parseNullableDate(body.get("nextDueDate")));
        v.setPreviousDate(parseNullableDate(body.get("previousDate")));
        v.setNotes(body.get("notes"));
        v.setStatus(normalizeVaccinationStatus(
                body.get("status"),
                v.getAdministeredDate(),
                v.getNextDueDate()));

        return vaccinations.save(v);
    }

    @PutMapping("/vaccinations/{id}/{username}")
    public VaccinationRecord updateVaccination(@PathVariable Long id,
                                               @PathVariable String username,
                                               @RequestBody Map<String,String> body) {

        User doctor=vet(username);

        VaccinationRecord v=vaccinations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Vaccination record not found"));

        assignedPet(doctor,v.getPet().getId());

        v.setVeterinarian(doctor);

        if(body.containsKey("vaccineName") && !body.get("vaccineName").trim().isEmpty())
            v.setVaccineName(body.get("vaccineName"));

        if(body.containsKey("vaccineType"))
            v.setVaccineType(body.get("vaccineType"));

        if(body.containsKey("administeredDate"))
            v.setAdministeredDate(parseNullableDate(body.get("administeredDate")));

        if(body.containsKey("nextDueDate"))
            v.setNextDueDate(parseNullableDate(body.get("nextDueDate")));

        if(body.containsKey("notes"))
            v.setNotes(body.get("notes"));

        v.setStatus(normalizeVaccinationStatus(
                body.get("status"),
                v.getAdministeredDate(),
                v.getNextDueDate()));

        return vaccinations.save(v);
    }

    @GetMapping("/prescriptions/{username}/{petCode}")
    public List<Prescription> petPrescriptions(@PathVariable String username,
                                               @PathVariable String petCode) {
        User doctor=vet(username);
        Pet pet=petFromCode(petCode);
        assignedPet(doctor,pet.getId());
        return prescriptions.findByPetOrderByIdDesc(pet);
    }

    @PostMapping("/prescriptions/{username}")
    public Prescription prescription(@PathVariable String username,
                                     @RequestBody Map<String, String> body) {

        User doctor=vet(username);
        Pet pet=petFromBody(doctor,body);

        Prescription p=new Prescription();
        p.setVeterinarian(doctor);
        p.setBranch(doctor.getBranch());
        p.setPet(pet);
        p.setMedicineName(required(body,"medicineName"));
        p.setDosage(body.get("dosage"));
        p.setFrequency(body.get("frequency"));
        p.setDuration(body.get("duration"));
        p.setInstructions(body.get("instructions"));

        return prescriptions.save(p);
    }

    private Pet petFromBody(User doctor,Map<String,String> body) {
        String petId=required(body,"petId");

        Pet pet;

        if(petId.toUpperCase().startsWith("PET-")) {
            pet=petFromCode(petId);
        } else {
            try {
                pet=pets.findById(Long.valueOf(petId))
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,"Pet not found"));
            } catch(NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid Pet ID.");
            }
        }

        return assignedPet(doctor,pet.getId());
    }

    private Pet petFromCode(String code) {
        String normalized=code.trim().toUpperCase();

        if(!normalized.matches("PET-\\d{6}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid Pet ID. Use format PET-000001.");

        long id=Long.parseLong(normalized.substring(4));

        return pets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Pet not found."));
    }

    private String required(Map<String,String> body,String key) {
        String value=body.get(key);
        if(value==null || value.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    key+" is required");
        return value.trim();
    }

    private LocalDate parseDateOrToday(String value) {
        if(value==null || value.trim().isEmpty())
            return LocalDate.now();

        return parseNullableDate(value);
    }

    private LocalDate parseNullableDate(String value) {
        if(value==null || value.trim().isEmpty())
            return null;

        try {
            return LocalDate.parse(value);
        } catch(Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid date. Use yyyy-MM-dd.");
        }
    }

    private String normalizeVaccinationStatus(String requested,
                                              LocalDate administered,
                                              LocalDate due) {

        if(requested!=null && !requested.trim().isEmpty()) {
            String s=requested.trim().toUpperCase();

            if("COMPLETED".equals(s))
                return "COMPLETED";

            if("OVERDUE".equals(s))
                return "OVERDUE";

            if("DUE".equals(s) || "PENDING".equals(s))
                return due!=null && due.isBefore(LocalDate.now())
                        ? "OVERDUE" : "PENDING";
        }

        if(administered!=null)
            return "COMPLETED";

        if(due!=null && due.isBefore(LocalDate.now()))
            return "OVERDUE";

        return "PENDING";
    }
}
