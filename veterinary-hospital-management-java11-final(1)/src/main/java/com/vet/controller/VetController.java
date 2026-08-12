package com.vet.controller;

import com.vet.model.*;
import com.vet.repository.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/vet")
public class VetController {
    private final UserRepository users; private final AppointmentRepository appointments; private final PetRepository pets;
    private final MedicalReportRepository reports; private final PrescriptionRepository prescriptions;
    public VetController(UserRepository u,AppointmentRepository a,PetRepository p,MedicalReportRepository r,PrescriptionRepository pr){users=u;appointments=a;pets=p;reports=r;prescriptions=pr;}

    private User vet(String username){return users.findByUsername(username).orElseThrow();}

    @GetMapping("/appointments/{username}") public List<Appointment> appointments(@PathVariable String username){return appointments.findByVeterinarianOrderByPriorityAscAppointmentDateAscAppointmentTimeAsc(vet(username));}
    @GetMapping("/all-priority-appointments") public List<Appointment> all(){return appointments.findAllPrioritySorted();}

    @PutMapping("/appointments/{id}/status")
    public Appointment status(@PathVariable Long id,@RequestBody Map<String,String> b){Appointment a=appointments.findById(id).orElseThrow();a.setStatus(AppointmentStatus.valueOf(b.get("status")));return appointments.save(a);}

    @GetMapping("/pets/{id}") public Pet pet(@PathVariable Long id){return pets.findById(id).orElseThrow();}
    @GetMapping("/reports/{username}") public List<MedicalReport> reports(@PathVariable String username){return reports.findByVeterinarian(vet(username));}

    @PostMapping("/reports/{username}")
    public MedicalReport createReport(@PathVariable String username,@RequestBody Map<String,String> b){
        MedicalReport r=new MedicalReport();r.setVeterinarian(vet(username));r.setPet(pets.findById(Long.valueOf(b.get("petId"))).orElseThrow());
        r.setVisitDate(LocalDate.now());r.setSymptoms(b.get("symptoms"));r.setObservations(b.get("observations"));r.setDiagnosis(b.get("diagnosis"));
        r.setTreatment(b.get("treatment"));r.setFollowUpDate(b.get("followUpDate"));r.setSurgeryRequired(Boolean.valueOf(b.getOrDefault("surgeryRequired","false")));
        r.setSurgeryDetails(b.get("surgeryDetails"));r.setNotes(b.get("notes"));r.setCompleted(true);return reports.save(r);
    }

    @PostMapping("/prescriptions/{username}")
    public Prescription prescription(@PathVariable String username,@RequestBody Map<String,String> b){
        Prescription p=new Prescription();p.setVeterinarian(vet(username));p.setPet(pets.findById(Long.valueOf(b.get("petId"))).orElseThrow());
        p.setMedicineName(b.get("medicineName"));p.setDosage(b.get("dosage"));p.setFrequency(b.get("frequency"));p.setDuration(b.get("duration"));p.setInstructions(b.get("instructions"));return prescriptions.save(p);
    }
}
