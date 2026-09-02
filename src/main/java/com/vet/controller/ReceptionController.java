package com.vet.controller;

import com.vet.model.*;
import com.vet.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reception")
public class ReceptionController {
    private final UserRepository users;
    private final PetRepository pets;
    private final AppointmentRepository appointments;
    private final GroomingRepository grooming;
    private final MedicalReportRepository reports;
    private final VaccinationRepository vaccinations;
    private final PrescriptionRepository prescriptions;
    private final VeterinarianAvailabilityRepository availability;
    private final OperationRepository operations;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    public ReceptionController(UserRepository users, PetRepository pets,
                                AppointmentRepository appointments, GroomingRepository grooming,
                                MedicalReportRepository reports, VaccinationRepository vaccinations,
                                PrescriptionRepository prescriptions,
                                VeterinarianAvailabilityRepository availability,
                                OperationRepository operations,
                                org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.users=users; this.pets=pets; this.appointments=appointments; this.grooming=grooming;
        this.reports=reports; this.vaccinations=vaccinations; this.prescriptions=prescriptions;
        this.availability=availability; this.operations=operations; this.encoder=encoder;
    }

    @GetMapping("/veterinarians")
    public List<Map<String,Object>> veterinarians() {
        return users.findByRole(Role.VETERINARIAN).stream().map(this::vetDto).collect(Collectors.toList());
    }

    @GetMapping("/owners")
    public List<Map<String,Object>> owners() {
        return users.findByRole(Role.PET_OWNER).stream().map(this::ownerDto).collect(Collectors.toList());
    }

    @GetMapping("/profile")
    public Map<String,Object> profile(Authentication authentication) {
        User u = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receptionist profile not found."));
        return receptionistDto(u);
    }

    @PostMapping("/owners/register-with-pet")
    public Map<String,Object> registerOwnerWithPet(@RequestBody Map<String,String> body) {
        String ownerName = required(body, "ownerName");
        String ownerPhone = required(body, "ownerPhone");
        String petName = required(body, "petName");
        String petSpecies = required(body, "petSpecies");

        /*
         * Username/password are NOT entered by the receptionist.
         * They are generated automatically after the owner is saved.
         */
        String temporaryUsername = "pending_" + UUID.randomUUID().toString().replace("-", "");
        String generatedPassword = "Pet@" + System.currentTimeMillis();

        User owner = new User();
        owner.setUsername(temporaryUsername);
        owner.setPassword(encoder.encode(generatedPassword));
        owner.setRole(Role.PET_OWNER);
        owner.setFullName(ownerName);
        owner.setAddress(body.get("ownerAddress"));
        owner.setEmail(body.get("ownerEmail"));
        owner.setPhone(ownerPhone);

        owner = users.save(owner);

        String generatedUsername = "owner" + owner.getId();
        owner.setUsername(generatedUsername);
        owner = users.save(owner);

        Pet pet = new Pet();
        pet.setName(petName);
        pet.setSpecies(petSpecies);
        pet.setBreed(body.get("petBreed"));
        pet.setGender(body.get("petGender"));
        pet.setAge(parseInteger(body.get("petAge")));
        pet.setWeight(parseDouble(body.get("petWeight")));
        pet.setColor(body.get("petColor"));
        pet.setAllergies(body.get("petAllergies"));
        pet.setMedicalConditions(body.get("petConditions"));
        pet.setOwner(owner);

        if (body.get("veterinarianId") != null && !body.get("veterinarianId").trim().isEmpty()) {
            pet.setAssignedVeterinarian(resolveVet(body.get("veterinarianId")));
        }

        pet = pets.save(pet);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("owner", ownerDto(owner));
        result.put("pet", petDto(pet));
        result.put("generatedUsername", generatedUsername);
        result.put("generatedPassword", generatedPassword);
        return result;
    }

    @PostMapping("/pets")
    public Map<String,Object> registerPet(@RequestBody Map<String,String> body) {
        User owner=resolveOwner(body.get("ownerId"));
        Pet pet=new Pet();
        pet.setName(required(body,"name")); pet.setSpecies(body.get("species")); pet.setBreed(body.get("breed"));
        pet.setGender(body.get("gender")); pet.setAge(parseInteger(body.get("age"))); pet.setWeight(parseDouble(body.get("weight")));
        pet.setColor(body.get("color")); pet.setAllergies(body.get("allergies")); pet.setMedicalConditions(body.get("medicalConditions"));
        pet.setOwner(owner);
        if(body.get("veterinarianId")!=null && !body.get("veterinarianId").trim().isEmpty())
            pet.setAssignedVeterinarian(resolveVet(body.get("veterinarianId")));
        return petDto(pets.save(pet));
    }

    @PutMapping("/pets/{id}/assign-veterinarian")
    public Map<String,Object> assignVeterinarian(@PathVariable Long id, @RequestBody Map<String,String> body) {
        Pet pet=pets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet not found."));
        pet.setAssignedVeterinarian(resolveVet(body.get("veterinarianId")));
        return petDto(pets.save(pet));
    }

    @GetMapping("/pets")
    public List<Map<String,Object>> pets(@RequestParam(required=false) String search) {
        String q=search==null?"":search.trim().toLowerCase();
        return pets.findAll().stream().filter(p -> q.isEmpty()
                || p.getPetCode().toLowerCase().contains(q)
                || safe(p.getName()).toLowerCase().contains(q)
                || safe(p.getOwner()==null?null:p.getOwner().getFullName()).toLowerCase().contains(q))
                .map(this::petDto).collect(Collectors.toList());
    }

    @GetMapping("/pets/{id}")
    public Map<String,Object> pet(@PathVariable Long id) {
        return petDto(pets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet not found.")));
    }

    @GetMapping("/appointments/week")
    public List<Map<String,Object>> weeklyAppointments() {
        LocalDate[] w=week();
        return appointments.findByAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(w[0],w[1])
                .stream().map(this::appointmentDto).collect(Collectors.toList());
    }

    @GetMapping("/grooming/week")
    public List<Map<String,Object>> weeklyGrooming() {
        LocalDate[] w=week();
        return grooming.findByBookingDateBetweenOrderByBookingDateAscBookingTimeAsc(w[0],w[1])
                .stream().map(this::groomingDto).collect(Collectors.toList());
    }

    @GetMapping("/veterinarians/availability")
    public List<Map<String,Object>> veterinarianAvailabilitySummary() {
        LocalDate[] w = week();

        return users.findByRole(Role.VETERINARIAN).stream().map(vet -> {
            Map<String,Object> m = vetDto(vet);

            List<Map<String,Object>> schedule = availability
                    .findByAvailabilityDateBetweenOrderByAvailabilityDateAsc(w[0], w[1])
                    .stream()
                    .filter(a -> a.getVeterinarian().getId().equals(vet.getId()))
                    .map(this::availabilityDto)
                    .collect(Collectors.toList());

            m.put("weekSchedule", schedule);
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/veterinarian-availability/week")
    public List<Map<String,Object>> weeklyAvailability() {
        LocalDate[] w=week();
        return availability.findByAvailabilityDateBetweenOrderByAvailabilityDateAsc(w[0],w[1])
                .stream().map(this::availabilityDto).collect(Collectors.toList());
    }

    @PostMapping("/veterinarian-availability")
    public Map<String,Object> saveAvailability(@RequestBody Map<String,String> body) {
        User vet=resolveVet(body.get("veterinarianId"));
        LocalDate date=parseDate(required(body,"date"));
        String status=required(body,"status").toUpperCase();
        Set<String> allowed=new HashSet<>(Arrays.asList("PRESENT","ABSENT","AVAILABLE","NOT_AVAILABLE","LEAVE"));
        if(!allowed.contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid availability status.");
        VeterinarianAvailability a=availability.findByVeterinarianAndAvailabilityDate(vet,date).orElseGet(VeterinarianAvailability::new);
        a.setVeterinarian(vet); a.setAvailabilityDate(date); a.setStatus(status);
        return availabilityDto(availability.save(a));
    }

    @GetMapping("/operations/week")
    public List<Map<String,Object>> weeklyOperations() {
        LocalDate[] w=week();
        return operations.findByOperationDateBetweenOrderByOperationDateAscOperationTimeAsc(w[0],w[1])
                .stream().map(this::operationDto).collect(Collectors.toList());
    }

    @PostMapping("/operations")
    public Map<String,Object> createOperation(@RequestBody Map<String,String> body) {
        Pet pet=pets.findById(Long.valueOf(required(body,"petId").replace("PET-","")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet not found."));
        Operation o=new Operation(); o.setPet(pet); o.setOwner(pet.getOwner());
        if(body.get("veterinarianId")!=null && !body.get("veterinarianId").trim().isEmpty()) o.setVeterinarian(resolveVet(body.get("veterinarianId")));
        o.setOperationType(required(body,"operationType")); o.setOperationDate(parseDate(required(body,"operationDate")));
        o.setOperationTime(parseTime(body.get("operationTime"))); o.setStatus(body.getOrDefault("status","SCHEDULED")); o.setNotes(body.get("notes"));
        return operationDto(operations.save(o));
    }

    @GetMapping("/medical-records")
    public List<Map<String,Object>> medicalRecords() {
        return reports.findAll().stream().map(this::reportDto).collect(Collectors.toList());
    }

    @GetMapping("/owners/{id}")
    public Map<String,Object> owner(@PathVariable Long id) {
        User u=users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Owner not found."));
        return ownerDto(u);
    }

    private User resolveOwner(String id) {
        try { User u=users.findById(Long.valueOf(requiredValue(id))).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Owner not found."));
            if(u.getRole()!=Role.PET_OWNER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selected user is not a pet owner."); return u;
        } catch(NumberFormatException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Owner ID.");}
    }
    private User resolveVet(String id) {
        try { User u=users.findById(Long.valueOf(requiredValue(id))).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Veterinarian not found."));
            if(u.getRole()!=Role.VETERINARIAN) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selected user is not a veterinarian."); return u;
        } catch(NumberFormatException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Veterinarian ID.");}
    }
    private String required(Map<String,String> b,String k){return requiredValue(b.get(k),k);}
    private String requiredValue(String v){return requiredValue(v,"value");}
    private String requiredValue(String v,String k){if(v==null||v.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,k+" is required."); return v.trim();}
    private Integer parseInteger(String v){if(v==null||v.trim().isEmpty()) return null; try{return Integer.valueOf(v);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid age.");}}
    private Double parseDouble(String v){if(v==null||v.trim().isEmpty()) return null; try{return Double.valueOf(v);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid weight.");}}
    private LocalDate parseDate(String v){try{return LocalDate.parse(v);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid date. Use yyyy-MM-dd.");}}
    private LocalTime parseTime(String v){if(v==null||v.trim().isEmpty()) return null; try{return LocalTime.parse(v);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid time. Use HH:mm.");}}
    private LocalDate[] week(){LocalDate today=LocalDate.now(); return new LocalDate[]{today.with(DayOfWeek.MONDAY),today.with(DayOfWeek.SUNDAY)};}
    private String safe(String s){return s==null?"":s;}

    private Map<String,Object> receptionistDto(User u) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("fullName", u.getFullName());
        m.put("role", u.getRole());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("address", u.getAddress());
        return m;
    }

    private Map<String,Object> vetDto(User u){Map<String,Object> m=new LinkedHashMap<>();m.put("id",u.getId());m.put("username",u.getUsername());m.put("fullName",u.getFullName());m.put("specialization",u.getSpecialization());m.put("qualification",u.getQualification());m.put("email",u.getEmail());m.put("phone",u.getPhone());m.put("availabilityStatus",u.getAvailabilityStatus());return m;}
    private Map<String,Object> ownerDto(User u){Map<String,Object> m=new LinkedHashMap<>();m.put("id",u.getId());m.put("username",u.getUsername());m.put("fullName",u.getFullName());m.put("address",u.getAddress());m.put("email",u.getEmail());m.put("phone",u.getPhone());m.put("pets",pets.findByOwner(u).stream().map(Pet::getPetCode).collect(Collectors.toList()));return m;}
    private Map<String,Object> petDto(Pet p){Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.getId());m.put("petCode",p.getPetCode());m.put("name",p.getName());m.put("species",p.getSpecies());m.put("breed",p.getBreed());m.put("gender",p.getGender());m.put("age",p.getAge());m.put("weight",p.getWeight());m.put("color",p.getColor());m.put("allergies",p.getAllergies());m.put("medicalConditions",p.getMedicalConditions());m.put("owner",p.getOwner()==null?null:ownerDtoShallow(p.getOwner()));m.put("assignedVeterinarian",p.getAssignedVeterinarian()==null?null:vetDto(p.getAssignedVeterinarian()));return m;}
    private Map<String,Object> ownerDtoShallow(User u){Map<String,Object> m=new LinkedHashMap<>();m.put("id",u.getId());m.put("fullName",u.getFullName());m.put("phone",u.getPhone());m.put("email",u.getEmail());return m;}
    private Map<String,Object> appointmentDto(Appointment a){Map<String,Object> m=new LinkedHashMap<>();m.put("id",a.getId());m.put("petId",a.getPet().getPetCode());m.put("petName",a.getPet().getName());m.put("owner",a.getOwner()==null?"-":a.getOwner().getFullName());m.put("veterinarian",a.getVeterinarian()==null?"-":a.getVeterinarian().getFullName());m.put("date",a.getAppointmentDate());m.put("time",a.getAppointmentTime());m.put("status",a.getStatus());m.put("reason",a.getReason());return m;}
    private Map<String,Object> groomingDto(GroomingBooking g){Map<String,Object> m=new LinkedHashMap<>();m.put("id",g.getId());m.put("petId",g.getPet().getPetCode());m.put("petName",g.getPet().getName());m.put("owner",g.getOwner().getFullName());m.put("service",g.getServiceName());m.put("date",g.getBookingDate());m.put("time",g.getBookingTime());m.put("status",g.getStatus());return m;}
    private Map<String,Object> availabilityDto(VeterinarianAvailability a){Map<String,Object> m=new LinkedHashMap<>();m.put("id",a.getId());m.put("veterinarianId",a.getVeterinarian().getId());m.put("veterinarian",a.getVeterinarian().getFullName());m.put("date",a.getAvailabilityDate());m.put("status",a.getStatus());return m;}
    private Map<String,Object> operationDto(Operation o){Map<String,Object> m=new LinkedHashMap<>();m.put("id",o.getId());m.put("petId",o.getPet().getPetCode());m.put("petName",o.getPet().getName());m.put("owner",o.getOwner().getFullName());m.put("operationType",o.getOperationType());m.put("date",o.getOperationDate());m.put("time",o.getOperationTime());m.put("veterinarian",o.getVeterinarian()==null?"-":o.getVeterinarian().getFullName());m.put("status",o.getStatus());m.put("notes",o.getNotes());return m;}
    private Map<String,Object> reportDto(MedicalReport r){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("petId",r.getPet().getPetCode());m.put("petName",r.getPet().getName());m.put("date",r.getVisitDate());m.put("diagnosis",r.getDiagnosis());m.put("treatment",r.getTreatment());m.put("symptoms",r.getSymptoms());m.put("notes",r.getNotes());m.put("veterinarian",r.getVeterinarian()==null?"-":r.getVeterinarian().getFullName());return m;}
}
