package com.vet.repository;

import com.vet.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface VaccinationRepository extends JpaRepository<VaccinationRecord,Long> {
    List<VaccinationRecord> findByPet(Pet pet);
}
