package com.vet.config;

import com.vet.model.Pet;
import com.vet.model.Role;
import com.vet.model.User;
import com.vet.repository.PetRepository;
import com.vet.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository users,
                           PetRepository pets,
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
        };
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
