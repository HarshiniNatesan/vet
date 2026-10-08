package com.vet.repository;

import com.vet.model.User;
import com.vet.model.VeterinarianAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface VeterinarianAvailabilityRepository extends JpaRepository<VeterinarianAvailability,Long> {
    List<VeterinarianAvailability> findByAvailabilityDateBetweenOrderByAvailabilityDateAsc(LocalDate start, LocalDate end);
    Optional<VeterinarianAvailability> findByVeterinarianAndAvailabilityDate(User veterinarian, LocalDate date);
}
