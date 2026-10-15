package com.vet.controller;

import com.vet.model.*;
import com.vet.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

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
        User staff=currentReception();
        return users.findByRole(Role.VETERINARIAN).stream().filter(v->branchAllowed(staff.getBranch(),v.getBranch())).map(this::vetDto).collect(Collectors.toList());
    }

    @GetMapping("/owners")
    public List<Map<String,Object>> owners() {
        User staff=currentReception();
        return users.findByRole(Role.PET_OWNER).stream().filter(o->ownerAllowed(staff,o)).map(this::ownerDto).collect(Collectors.toList());
    }

    @GetMapping("/profile")
    public Map<String,Object> profile(Authentication authentication) {
        User u = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receptionist profile not found."));
        return receptionistDto(u);
    }

    @PostMapping("/owners/register-with-pet")
    @Transactional
    public Map<String,Object> registerOwnerWithPet(@RequestBody Map<String,String> body) {
        currentReception();
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
            requirePetAccess(pet);
        pet.setAssignedVeterinarian(resolveVet(body.get("veterinarianId")));
        }

        pet = pets.save(pet);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("owner", ownerDto(owner));
        result.put("pet", petDto(pet));
        result.put("generatedUsername", generatedUsername);
        result.put("generatedPassword", generatedPassword);
        result.put("savedToDatabase", true);
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
        User staff=currentReception(); String q=search==null?"":search.trim().toLowerCase();
        return pets.findAll().stream().filter(p->petAllowed(staff,p)).filter(p -> q.isEmpty()
                || p.getPetCode().toLowerCase().contains(q)
                || safe(p.getName()).toLowerCase().contains(q)
                || safe(p.getOwner()==null?null:p.getOwner().getFullName()).toLowerCase().contains(q))
                .map(this::petDto).collect(Collectors.toList());
    }

    @GetMapping("/pets/{id}")
    public Map<String,Object> pet(@PathVariable Long id) {
        Pet p=pets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet not found."));
        requirePetAccess(p);
        return petDto(p);
    }


    @PutMapping("/owners/{id}")
    public Map<String,Object> updateOwner(@PathVariable Long id,@RequestBody Map<String,String> body) {
        User owner=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet owner not found."));
        if(owner.getRole()!=Role.PET_OWNER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Not a pet owner.");
        if(!ownerAllowed(currentReception(),owner)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Owner belongs to another branch.");
        if(body.containsKey("fullName") && !body.get("fullName").trim().isEmpty()) owner.setFullName(body.get("fullName").trim());
        if(body.containsKey("email")) owner.setEmail(body.get("email"));
        if(body.containsKey("phone")) owner.setPhone(body.get("phone"));
        if(body.containsKey("address")) owner.setAddress(body.get("address"));
        return ownerDto(users.save(owner));
    }

    @PutMapping("/pets/{id}")
    public Map<String,Object> updatePet(@PathVariable Long id,@RequestBody Map<String,String> body) {
        Pet pet=pets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet not found."));
        requirePetAccess(pet);
        if(body.containsKey("name") && !body.get("name").trim().isEmpty()) pet.setName(body.get("name").trim());
        if(body.containsKey("species")) pet.setSpecies(body.get("species"));
        if(body.containsKey("breed")) pet.setBreed(body.get("breed"));
        if(body.containsKey("gender")) pet.setGender(body.get("gender"));
        if(body.containsKey("age")) pet.setAge(parseInteger(body.get("age")));
        if(body.containsKey("weight")) pet.setWeight(parseDouble(body.get("weight")));
        if(body.containsKey("color")) pet.setColor(body.get("color"));
        if(body.containsKey("allergies")) pet.setAllergies(body.get("allergies"));
        if(body.containsKey("medicalConditions")) pet.setMedicalConditions(body.get("medicalConditions"));
        if(body.containsKey("veterinarianId") && body.get("veterinarianId")!=null && !body.get("veterinarianId").trim().isEmpty())
            pet.setAssignedVeterinarian(resolveVet(body.get("veterinarianId")));
        return petDto(pets.save(pet));
    }

    @GetMapping("/appointments/week")
    public List<Map<String,Object>> weeklyAppointments() {
        LocalDate[] w=week();
        User staff=currentReception(); return appointments.findByAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(w[0],w[1])
                .stream().filter(a->branchAllowed(staff.getBranch(),a.getBranch())).map(this::appointmentDto).collect(Collectors.toList());
    }

    @GetMapping("/grooming/week")
    public List<Map<String,Object>> weeklyGrooming() {
        LocalDate[] w=week();
        User staff=currentReception(); return grooming.findByBookingDateBetweenOrderByBookingDateAscBookingTimeAsc(w[0],w[1])
                .stream().filter(g->petAllowed(staff,g.getPet())).map(this::groomingDto).collect(Collectors.toList());
    }

    @GetMapping("/veterinarians/availability")
    public List<Map<String,Object>> veterinarianAvailabilitySummary() {
        LocalDate[] w = week();

        User staff=currentReception();
        return users.findByRole(Role.VETERINARIAN).stream().filter(v->branchAllowed(staff.getBranch(),v.getBranch())).map(vet -> {
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
        User staff=currentReception(); return availability.findByAvailabilityDateBetweenOrderByAvailabilityDateAsc(w[0],w[1])
                .stream().filter(a->branchAllowed(staff.getBranch(),a.getVeterinarian().getBranch())).map(this::availabilityDto).collect(Collectors.toList());
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

    @GetMapping("/stats")
    public Map<String,Object> stats() {
        User staff=currentReception();
        long petCount=pets.findAll().stream().filter(p->petAllowed(staff,p)).count();
        long appointmentCount=appointments.findAll().stream().filter(a->branchAllowed(staff.getBranch(),a.getBranch())).count();
        long vetCount=users.findByRole(Role.VETERINARIAN).stream().filter(v->branchAllowed(staff.getBranch(),v.getBranch())).count();
        long groomingCount=grooming.findAll().stream().filter(g->petAllowed(staff,g.getPet())).count();
        long reportCount=reports.findAll().stream().filter(r->branchAllowed(staff.getBranch(),r.getBranch())).count();
        Map<String,Object> x=new LinkedHashMap<>();
        x.put("petOwners",users.findByRole(Role.PET_OWNER).stream().filter(o->ownerAllowed(staff,o)).count());
        x.put("pets",petCount);
        x.put("appointments",appointmentCount);
        x.put("completedAppointments",appointments.findAll().stream().filter(a->branchAllowed(staff.getBranch(),a.getBranch())&&a.getStatus()==AppointmentStatus.COMPLETED).count());
        x.put("pendingAppointments",appointments.findAll().stream().filter(a->branchAllowed(staff.getBranch(),a.getBranch())&&a.getStatus()==AppointmentStatus.PENDING).count());
        x.put("veterinarians",vetCount);
        x.put("groomingBookings",groomingCount);
        x.put("medicalReports",reportCount);
        return x;
    }

    @GetMapping("/operations/week")
    public List<Map<String,Object>> weeklyOperations() {
        LocalDate[] w=week();
        User staff=currentReception(); return operations.findByOperationDateBetweenOrderByOperationDateAscOperationTimeAsc(w[0],w[1])
                .stream().filter(o->petAllowed(staff,o.getPet())).map(this::operationDto).collect(Collectors.toList());
    }

    @PostMapping("/operations")
    public Map<String,Object> createOperation(@RequestBody Map<String,String> body) {
        Pet pet=pets.findById(Long.valueOf(required(body,"petId").replace("PET-","")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Pet not found."));
        requirePetAccess(pet);
        Operation o=new Operation(); o.setPet(pet); o.setOwner(pet.getOwner());
        if(body.get("veterinarianId")!=null && !body.get("veterinarianId").trim().isEmpty()) o.setVeterinarian(resolveVet(body.get("veterinarianId")));
        o.setOperationType(required(body,"operationType")); o.setOperationDate(parseDate(required(body,"operationDate")));
        o.setOperationTime(parseTime(body.get("operationTime"))); o.setStatus(body.getOrDefault("status","SCHEDULED")); o.setNotes(body.get("notes"));
        return operationDto(operations.save(o));
    }

    @GetMapping("/medical-records")
    public List<Map<String,Object>> medicalRecords() {
        User staff=currentReception(); return reports.findAll().stream().filter(r->branchAllowed(staff.getBranch(),r.getBranch())).map(this::reportDto).collect(Collectors.toList());
    }

    @GetMapping("/owners/{id}")
    public Map<String,Object> owner(@PathVariable Long id) {
        User u=users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Owner not found."));
        if(!ownerAllowed(currentReception(),u)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Owner belongs to another branch.");
        return ownerDto(u);
    }

    private User currentReception(){
        Authentication a=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        User u=users.findByUsername(a.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Receptionist account not found."));
        if(u.getRole()!=Role.RECEPTIONIST) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Receptionist access required.");
        return u;
    }
    private boolean branchAllowed(Branch staff, Branch data){return staff==null||data==null||staff.getId().equals(data.getId());}
    private boolean petAllowed(User staff, Pet p){return p!=null && (staff.getBranch()==null || p.getAssignedVeterinarian()==null || p.getAssignedVeterinarian().getBranch()==null || staff.getBranch().getId().equals(p.getAssignedVeterinarian().getBranch().getId()));}
    private void requirePetAccess(Pet p){if(!petAllowed(currentReception(),p))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Pet belongs to another branch.");}
    private boolean ownerAllowed(User staff, User owner){if(staff.getBranch()==null)return true;List<Pet> ps=pets.findByOwner(owner);if(ps.isEmpty())return true;return ps.stream().anyMatch(p->petAllowed(staff,p));}

    private User resolveOwner(String id) {
        try { User u=users.findById(Long.valueOf(requiredValue(id))).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Owner not found."));
            if(u.getRole()!=Role.PET_OWNER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selected user is not a pet owner."); if(!ownerAllowed(currentReception(),u)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Owner belongs to another branch."); return u;
        } catch(NumberFormatException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Owner ID.");}
    }
    private User resolveVet(String id) {
        try { User u=users.findById(Long.valueOf(requiredValue(id))).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Veterinarian not found."));
            if(u.getRole()!=Role.VETERINARIAN) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selected user is not a veterinarian."); if(!branchAllowed(currentReception().getBranch(),u.getBranch())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Veterinarian belongs to another branch."); return u;
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
