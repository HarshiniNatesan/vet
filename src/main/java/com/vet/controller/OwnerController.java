package com.vet.controller;

import com.vet.model.*;
import com.vet.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/owner")
public class OwnerController {

    private final UserRepository users;
    private final PetRepository pets;
    private final AppointmentRepository appointments;
    private final MedicalReportRepository reports;
    private final VaccinationRepository vaccinations;
    private final GroomingRepository grooming;
    private final PrescriptionRepository prescriptions;
    private final VeterinarianAvailabilityRepository availability;
    private final PasswordEncoder passwordEncoder;

    public OwnerController(UserRepository u, PetRepository p, AppointmentRepository a,
                           MedicalReportRepository r, VaccinationRepository v,
                           GroomingRepository g,
                           PrescriptionRepository pr, VeterinarianAvailabilityRepository va, PasswordEncoder pe) {
        users=u;
        pets=p;
        appointments=a;
        reports=r;
        vaccinations=v;
        grooming=g;
        prescriptions=pr;
        availability=va;
        passwordEncoder=pe;
    }

    private User owner(String username){
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !username.equals(auth.getName()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can access only your own owner account");

        User u=users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Owner not found"));

        if(u.getRole()!=Role.PET_OWNER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not a pet owner");

        return u;
    }

    private Pet ownerPet(User owner, Long petId){
        Pet p=pets.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet not found"));

        if(p.getOwner()==null || !p.getOwner().getId().equals(owner.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This pet does not belong to the owner");

        return p;
    }

    @GetMapping("/pets/{username}")
    public List<Pet> pets(@PathVariable String username){
        return pets.findByOwner(owner(username));
    }

    /*
     * Pet creation has intentionally been removed from the owner module.
     * Pets must be registered by the receptionist.
     */

    @GetMapping("/veterinarians")
    public List<User> vets(){
        return users.findByRole(Role.VETERINARIAN);
    }

    @GetMapping("/appointments/{username}")
    public List<Appointment> appointments(@PathVariable String username){
        return appointments.findByOwnerOrderByAppointmentDateAscAppointmentTimeAsc(owner(username));
    }

    @PostMapping("/appointments/{username}")
    public Appointment book(@PathVariable String username,
                            @RequestBody Map<String,String> b){

        User o=owner(username);
        Pet p=ownerPet(o, Long.valueOf(required(b,"petId")));

        User v=users.findById(Long.valueOf(required(b,"vetId")))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Veterinarian not found"));

        if(v.getRole()!=Role.VETERINARIAN)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selected user is not a veterinarian");

        LocalDate appointmentDate=parseDate(required(b,"date"));
        LocalTime appointmentTime=parseTime(required(b,"time"));

        LocalDateTime selected=LocalDateTime.of(appointmentDate,appointmentTime);

        if(selected.isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Appointment date and time cannot be in the past.");

        String currentAvailability=v.getAvailabilityStatus();
        if(currentAvailability!=null &&
           ("ABSENT".equalsIgnoreCase(currentAvailability) ||
            "UNAVAILABLE".equalsIgnoreCase(currentAvailability) ||
            "NOT AVAILABLE".equalsIgnoreCase(currentAvailability) ||
            "LEAVE".equalsIgnoreCase(currentAvailability))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The selected veterinarian is currently unavailable.");
        }

        VeterinarianAvailability dayAvailability = availability
                .findByVeterinarianAndAvailabilityDate(v, appointmentDate)
                .orElse(null);
        if(dayAvailability!=null &&
           ("ABSENT".equalsIgnoreCase(dayAvailability.getStatus()) ||
            "NOT_AVAILABLE".equalsIgnoreCase(dayAvailability.getStatus()) ||
            "UNAVAILABLE".equalsIgnoreCase(dayAvailability.getStatus()) ||
            "LEAVE".equalsIgnoreCase(dayAvailability.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The selected veterinarian is unavailable on the selected date.");
        }

        Appointment a=new Appointment();
        a.setOwner(o);
        a.setPet(p);
        a.setVeterinarian(v);
        a.setAppointmentDate(appointmentDate);
        a.setAppointmentTime(appointmentTime);
        a.setReason(b.get("reason"));
        a.setPriority(priority(b.get("reason")));
        a.setStatus(AppointmentStatus.PENDING);

        return appointments.save(a);
    }

    @PutMapping("/appointments/{id}/cancel")
    public Appointment cancelAppointment(@PathVariable Long id,
                                         @RequestParam String username){

        User o=owner(username);

        Appointment a=appointments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Appointment not found"));

        if(a.getOwner()==null || !a.getOwner().getId().equals(o.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can cancel only your own appointments");

        if(a.getStatus()==AppointmentStatus.COMPLETED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Completed appointments cannot be cancelled");

        if(a.getStatus()==AppointmentStatus.CANCELLED)
            return a;

        a.setStatus(AppointmentStatus.CANCELLED);
        return appointments.save(a);
    }

    private AppointmentPriority priority(String reason){
        String s=reason==null?"":reason.toLowerCase();

        if(s.matches(".*(breathing|unconscious|seizure|poison|bleeding|accident|critical).*"))
            return AppointmentPriority.EMERGENCY;

        if(s.matches(".*(vomit|high fever|severe|injury|pain|not eating|weak).*"))
            return AppointmentPriority.HIGH;

        if(s.matches(".*(rash|itch|cough|cold|diarrhea).*"))
            return AppointmentPriority.NORMAL;

        return AppointmentPriority.ROUTINE;
    }

    @GetMapping("/reports/{username}/{petId}")
    public List<MedicalReport> reports(@PathVariable String username,
                                       @PathVariable Long petId){
        User o=owner(username);
        Pet p=ownerPet(o,petId);
        return reports.findByPetAndCompletedTrue(p);
    }

    @GetMapping("/vaccinations/{username}/{petId}")
    public List<VaccinationRecord> vaccinations(@PathVariable String username,
                                                @PathVariable Long petId){
        User o=owner(username);
        Pet p=ownerPet(o,petId);
        return vaccinations.findByPet(p);
    }

    @GetMapping("/prescriptions/{username}/{petId}")
    public List<Prescription> prescriptions(@PathVariable String username,
                                            @PathVariable Long petId){
        User o=owner(username);
        Pet p=ownerPet(o,petId);
        return prescriptions.findByPetOrderByIdDesc(p);
    }

    @GetMapping("/grooming/{username}")
    public List<GroomingBooking> grooming(@PathVariable String username){
        return grooming.findByOwnerOrderByBookingDateAscBookingTimeAsc(owner(username));
    }

    @PostMapping("/grooming/{username}")
    public GroomingBooking bookGrooming(@PathVariable String username,
                                        @RequestBody Map<String,String> b){

        User o=owner(username);
        Pet p=ownerPet(o,Long.valueOf(required(b,"petId")));

        LocalDate d=parseDate(required(b,"date"));
        LocalTime t=parseTime(required(b,"time"));

        if(LocalDateTime.of(d,t).isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Spa & grooming date and time cannot be in the past.");

        GroomingBooking g=new GroomingBooking();
        g.setOwner(o);
        g.setPet(p);
        g.setServiceName(required(b,"serviceName"));
        g.setBookingDate(d);
        g.setBookingTime(t);
        g.setStatus("PENDING");

        return grooming.save(g);
    }

    @PutMapping("/grooming/{id}/cancel")
    public GroomingBooking cancelGrooming(@PathVariable Long id,
                                          @RequestParam String username){

        User o=owner(username);

        GroomingBooking g=grooming.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Grooming booking not found"));

        if(g.getOwner()==null || !g.getOwner().getId().equals(o.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can cancel only your own grooming bookings");

        if("COMPLETED".equalsIgnoreCase(g.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Completed grooming bookings cannot be cancelled");

        g.setStatus("CANCELLED");
        return grooming.save(g);
    }


    @GetMapping("/profile/{username}")
    public Map<String,Object> profile(@PathVariable String username){
        User o=owner(username);
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",o.getId()); m.put("username",o.getUsername()); m.put("fullName",o.getFullName());
        m.put("email",o.getEmail()); m.put("phone",o.getPhone()); m.put("address",o.getAddress());
        return m;
    }

    @PutMapping("/profile/{username}")
    public Map<String,Object> updateProfile(@PathVariable String username,@RequestBody Map<String,String> b){
        User o=owner(username);
        if(b.containsKey("fullName") && !b.get("fullName").trim().isEmpty()) o.setFullName(b.get("fullName").trim());
        if(b.containsKey("email")) o.setEmail(b.get("email"));
        if(b.containsKey("phone")) o.setPhone(b.get("phone"));
        if(b.containsKey("address")) o.setAddress(b.get("address"));
        users.save(o);
        return profile(username);
    }

    @PutMapping("/pets/{username}/{id}")
    public Pet updatePet(@PathVariable String username,@PathVariable Long id,@RequestBody Map<String,String> b){
        User o=owner(username);
        Pet p=pets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet not found"));
        if(p.getOwner()==null || !p.getOwner().getId().equals(o.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can edit only your own pets.");
        if(b.containsKey("name") && !b.get("name").trim().isEmpty()) p.setName(b.get("name").trim());
        if(b.containsKey("breed")) p.setBreed(b.get("breed"));
        if(b.containsKey("color")) p.setColor(b.get("color"));
        if(b.containsKey("weight")) { try { p.setWeight(Double.valueOf(b.get("weight"))); } catch(Exception ignored){} }
        if(b.containsKey("allergies")) p.setAllergies(b.get("allergies"));
        if(b.containsKey("medicalConditions")) p.setMedicalConditions(b.get("medicalConditions"));
        return pets.save(p);
    }

    private String required(Map<String,String> body,String key){
        String value=body.get(key);
        if(value==null || value.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    key+" is required");
        return value.trim();
    }

    private LocalDate parseDate(String value){
        try {
            return LocalDate.parse(value);
        } catch(Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid date. Use yyyy-MM-dd.");
        }
    }

    private LocalTime parseTime(String value){
        try {
            return LocalTime.parse(value);
        } catch(Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid time. Use HH:mm.");
        }
    }


    @PutMapping("/change-password")
    public Map<String,String> changePassword(@RequestBody Map<String,String> body,
                                             Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        User current = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (current.getRole() != Role.PET_OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a pet owner");
        }

        String currentPassword = body.getOrDefault("currentPassword", "");
        String newPassword = body.getOrDefault("newPassword", "");
        String confirmPassword = body.getOrDefault("confirmPassword", "");

        if (!passwordEncoder.matches(currentPassword, current.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (newPassword.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be empty");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password and confirmation password do not match");
        }

        current.setPassword(passwordEncoder.encode(newPassword));
        users.save(current);
        return Map.of("message", "Password changed successfully");
    }
}
