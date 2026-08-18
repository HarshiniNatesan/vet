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

            User owner = users.findByUsername("owner1").orElseGet(() ->
                    users.save(new User(
                            "owner1",
                            encoder.encode("owner123"),
                            Role.PET_OWNER,
                            "Harshu Owner",
                            "owner@example.com",
                            "9876543210"
                    ))
            );

            User vet = users.findByUsername("doctor1").orElseGet(() ->
                    users.save(new User(
                            "doctor1",
                            encoder.encode("doctor123"),
                            Role.VETERINARIAN,
                            "Dr. Arun Kumar",
                            "doctor@example.com",
                            "9876500000"
                    ))
            );

            vet.setSpecialization("Small Animal Medicine");
            vet.setQualification("BVSc & AH");
            vet.setAvailabilityStatus("AVAILABLE");
            users.save(vet);

            if (!users.findByUsername("admin").isPresent()) {
                users.save(new User(
                        "admin",
                        encoder.encode("admin123"),
                        Role.ADMIN,
                        "System Admin",
                        "admin@example.com",
                        "9000000001"
                ));
            }

            if (!users.findByUsername("reception").isPresent()) {
                users.save(new User(
                        "reception",
                        encoder.encode("reception123"),
                        Role.RECEPTIONIST,
                        "Reception Staff",
                        "reception@example.com",
                        "9000000002"
                ));
            }

            if (!users.findByUsername("pharmacy").isPresent()) {
                users.save(new User(
                        "pharmacy",
                        encoder.encode("pharmacy123"),
                        Role.PHARMACY,
                        "Pharmacy Staff",
                        "pharmacy@example.com",
                        "9000000003"
                ));
            }

            /*
             * Create a test patient if none exists for owner1.
             * Also ALWAYS ensure the test patient is assigned to doctor1.
             * This fixes the situation where the veterinarian dashboard was empty.
             */
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
}
