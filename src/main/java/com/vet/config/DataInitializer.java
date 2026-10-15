package com.vet.config;

import com.vet.model.Pet;
import com.vet.model.Medicine;
import com.vet.model.Role;
import com.vet.model.User;
import com.vet.model.Branch;
import com.vet.repository.PetRepository;
import com.vet.repository.MedicineRepository;
import com.vet.repository.UserRepository;
import com.vet.repository.BranchRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository users,
                           PetRepository pets,
                           MedicineRepository medicines,
                           BranchRepository branches,
                           PasswordEncoder encoder) {
        return args -> {
            User owner = ensureDemoUser(users, encoder, "owner1", "owner123", Role.PET_OWNER,
                    "Harshu Owner", "owner@example.com", "9876543210");
            User vet = ensureDemoUser(users, encoder, "doctor1", "doctor123", Role.VETERINARIAN,
                    "Dr. Arun Kumar", "doctor@example.com", "9876500000");
            User admin = ensureDemoUser(users, encoder, "admin", "admin123", Role.ADMIN,
                    "System Admin", "admin@example.com", "9000000001");
            User reception = ensureDemoUser(users, encoder, "reception", "reception123", Role.RECEPTIONIST,
                    "Reception Staff", "reception@example.com", "9000000002");
            User pharmacy = ensureDemoUser(users, encoder, "pharmacy", "pharmacy123", Role.PHARMACY,
                    "Pharmacy Staff", "pharmacy@example.com", "9000000003");

            Branch tambaram = ensureBranch(branches, "TAMBARAM", "Tambaram Branch", "Tambaram, Chennai");
            ensureBranch(branches, "ECR", "ECR Branch", "ECR, Chennai");
            vet.setBranch(tambaram); vet.setConsultationFee(new java.math.BigDecimal("500.00"));
            reception.setBranch(tambaram); pharmacy.setBranch(tambaram);
            users.save(vet); users.save(reception); users.save(pharmacy);

            vet.setSpecialization("Small Animal Medicine");
            vet.setQualification("BVSc & AH");
            vet.setAvailabilityStatus("AVAILABLE");
            users.save(vet);

            // Ensure the built-in demo accounts are active even when the database
            // was created by an older version of the project.
            owner.setActive(true);
            vet.setActive(true);
            admin.setActive(true);
            reception.setActive(true);
            pharmacy.setActive(true);
            users.save(owner);
            users.save(vet);
            users.save(admin);
            users.save(reception);
            users.save(pharmacy);

            Pet pet;
            if (pets.findByOwner(owner).isEmpty()) {
                pet = new Pet();
                pet.setName("Bruno");
                pet.setSpecies("Dog");
                pet.setBreed("Labrador");
                pet.setGender("Male");
                pet.setAge(3);
                pet.setWeight(22.5);
                pet.setColor("Golden");
                pet.setAllergies("None");
                pet.setMedicalConditions("None");
                pet.setOwner(owner);
            } else {
                pet = pets.findByOwner(owner).get(0);
            }

            if (pet.getAssignedVeterinarian() == null ||
                    !vet.getId().equals(pet.getAssignedVeterinarian().getId())) {
                pet.setAssignedVeterinarian(vet);
            }
            pets.save(pet);

            // Seed a small pharmacy inventory for the first run.
            // Existing medicines are left untouched so real inventory is not overwritten.
            addMedicine(medicines, "Amoxicillin", "Antibiotic", "VetPharm", "AMX-2026-01", "2027-06-30", 120, 30, 85.00, "Broad-spectrum antibiotic.");
            addMedicine(medicines, "Carprofen", "NSAID", "AnimalCare", "CAR-2026-02", "2027-04-30", 75, 20, 120.00, "Veterinary anti-inflammatory medicine.");
            addMedicine(medicines, "Cefpodoxime", "Antibiotic", "VetPharm", "CEF-2026-03", "2027-03-31", 18, 20, 145.00, "Antibiotic; sample stock intentionally below reorder level.");
            addMedicine(medicines, "Metronidazole", "Antiprotozoal", "AnimalCare", "MET-2026-04", "2027-05-31", 10, 20, 65.00, "Used for selected veterinary infections.");
            addMedicine(medicines, "Prednisolone", "Corticosteroid", "VetPharm", "PRE-2026-05", "2027-08-31", 45, 15, 55.00, "Veterinary corticosteroid.");
            addMedicine(medicines, "Ondansetron", "Antiemetic", "AnimalCare", "OND-2026-06", "2027-07-31", 60, 15, 40.00, "Antiemetic medicine.");
        };
    }

    private Branch ensureBranch(BranchRepository branches, String code, String name, String address) {
        return branches.findByBranchCodeIgnoreCase(code).orElseGet(() -> {
            Branch b=new Branch(); b.setBranchCode(code); b.setBranchName(name); b.setAddress(address); b.setActive(true); return branches.save(b);
        });
    }

    private void addMedicine(MedicineRepository medicines, String name, String category, String manufacturer,
                             String batchNumber, String expiryDate, int quantity, int reorderLevel,
                             double unitPrice, String description) {
        if (medicines.findFirstByNameIgnoreCaseAndActiveTrue(name).isPresent()) return;
        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setCategory(category);
        medicine.setManufacturer(manufacturer);
        medicine.setBatchNumber(batchNumber);
        medicine.setExpiryDate(java.time.LocalDate.parse(expiryDate));
        medicine.setQuantity(quantity);
        medicine.setReorderLevel(reorderLevel);
        medicine.setUnitPrice(unitPrice);
        medicine.setDescription(description);
        medicine.setActive(true);
        medicines.save(medicine);
    }

    private User ensureDemoUser(UserRepository users,
                                PasswordEncoder encoder,
                                String username,
                                String password,
                                Role role,
                                String fullName,
                                String email,
                                String phone) {
        User user = users.findByUsername(username).orElseGet(() ->
                new User(username, encoder.encode(password), role, fullName, email, phone));

        // Keep these five demo credentials predictable for project evaluation.
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setActive(true);
        return users.save(user);
    }
}
