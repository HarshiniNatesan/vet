package com.vet.config;

import com.vet.model.Pet;
import com.vet.model.Role;
import com.vet.model.User;
import com.vet.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;

@Configuration
public class DataInitializer {
    @Bean CommandLineRunner init(UserRepository users, PetRepository pets, PasswordEncoder encoder) {
        return args -> {
            if (users.count() == 0) {
                User owner = users.save(new User("owner1", encoder.encode("owner123"), Role.PET_OWNER, "Harshu Owner", "owner@example.com", "9876543210"));
                User vet = users.save(new User("doctor1", encoder.encode("doctor123"), Role.VETERINARIAN, "Dr. Arun Kumar", "doctor@example.com", "9876500000"));
                users.save(new User("admin", encoder.encode("admin123"), Role.ADMIN, "System Admin", "admin@example.com", "9000000001"));
                users.save(new User("reception", encoder.encode("reception123"), Role.RECEPTIONIST, "Reception Staff", "reception@example.com", "9000000002"));
                users.save(new User("pharmacy", encoder.encode("pharmacy123"), Role.PHARMACY, "Pharmacy Staff", "pharmacy@example.com", "9000000003"));
                Pet p = new Pet();
                p.setName("Bruno"); p.setSpecies("Dog"); p.setBreed("Labrador"); p.setGender("Male"); p.setAge(3);
                p.setWeight(22.5); p.setColor("Golden"); p.setAllergies("None"); p.setMedicalConditions("None"); p.setOwner(owner);
                pets.save(p);
            }
        };
    }
}
