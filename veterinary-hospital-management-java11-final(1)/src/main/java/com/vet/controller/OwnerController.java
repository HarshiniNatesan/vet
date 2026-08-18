package com.vet.controller;

import com.vet.model.*;
import com.vet.repository.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/owner")
public class OwnerController {
    private final UserRepository users; private final PetRepository pets; private final AppointmentRepository appointments;
    private final MedicalReportRepository reports; private final VaccinationRepository vaccinations; private final GroomingRepository grooming;
    public OwnerController(UserRepository u, PetRepository p, AppointmentRepository a, MedicalReportRepository r, VaccinationRepository v, GroomingRepository g){users=u;pets=p;appointments=a;reports=r;vaccinations=v;grooming=g;}

    private User owner(String username){return users.findByUsername(username).orElseThrow();}

    @GetMapping("/pets/{username}") public List<Pet> pets(@PathVariable String username){return pets.findByOwner(owner(username));}
    @PostMapping("/pets/{username}") public Pet addPet(@PathVariable String username,@RequestBody Pet p){p.setId(null);p.setOwner(owner(username));return pets.save(p);}

    @GetMapping("/veterinarians") public List<User> vets(){return users.findByRole(Role.VETERINARIAN);}

    @GetMapping("/appointments/{username}") public List<Appointment> appointments(@PathVariable String username){return appointments.findByOwnerOrderByAppointmentDateAscAppointmentTimeAsc(owner(username));}

    @PostMapping("/appointments/{username}")
    public Appointment book(@PathVariable String username,@RequestBody Map<String,String> b){
        User o=owner(username); Pet p=pets.findById(Long.valueOf(b.get("petId"))).orElseThrow();
        Appointment a=new Appointment(); a.setOwner(o);a.setPet(p);
        if(b.get("vetId")!=null&&!b.get("vetId").isEmpty()) a.setVeterinarian(users.findById(Long.valueOf(b.get("vetId"))).orElseThrow());
        a.setAppointmentDate(LocalDate.parse(b.get("date"))); a.setAppointmentTime(LocalTime.parse(b.get("time")));
        a.setReason(b.get("reason")); a.setPriority(priority(b.get("reason"))); a.setStatus(AppointmentStatus.PENDING);
        return appointments.save(a);
    }

    private AppointmentPriority priority(String reason){
        String s=reason==null?"":reason.toLowerCase();
        if(s.matches(".*(breathing|unconscious|seizure|poison|bleeding|accident|critical).*")) return AppointmentPriority.EMERGENCY;
        if(s.matches(".*(vomit|high fever|severe|injury|pain|not eating|weak).*")) return AppointmentPriority.HIGH;
        if(s.matches(".*(rash|itch|cough|cold|diarrhea).*")) return AppointmentPriority.NORMAL;
        return AppointmentPriority.ROUTINE;
    }

    @GetMapping("/reports/{username}/{petId}") public List<MedicalReport> reports(@PathVariable String username,@PathVariable Long petId){
        Pet p=pets.findById(petId).orElseThrow(); if(!p.getOwner().getId().equals(owner(username).getId())) throw new RuntimeException("Forbidden"); return reports.findByPetAndCompletedTrue(p);
    }
    @GetMapping("/vaccinations/{username}/{petId}") public List<VaccinationRecord> vaccinations(@PathVariable String username,@PathVariable Long petId){return vaccinations.findByPet(pets.findById(petId).orElseThrow());}
    @GetMapping("/grooming/{username}") public List<GroomingBooking> grooming(@PathVariable String username){return grooming.findByOwnerOrderByBookingDateAscBookingTimeAsc(owner(username));}

    @PostMapping("/grooming/{username}") public GroomingBooking bookGrooming(@PathVariable String username,@RequestBody Map<String,String> b){
        User o=owner(username); GroomingBooking g=new GroomingBooking(); g.setOwner(o);g.setPet(pets.findById(Long.valueOf(b.get("petId"))).orElseThrow());
        g.setServiceName(b.get("serviceName"));g.setBookingDate(LocalDate.parse(b.get("date")));g.setBookingTime(LocalTime.parse(b.get("time")));
        g.setRequirements(b.get("requirements"));g.setStatus("PENDING");return grooming.save(g);
    }
}
