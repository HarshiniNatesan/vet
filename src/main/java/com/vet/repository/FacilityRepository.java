package com.vet.repository;

import com.vet.model.FacilityRecord;
import com.vet.model.Pet;
import com.vet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface FacilityRepository extends JpaRepository<FacilityRecord, Long> {
    List<FacilityRecord> findByPetOrderByServiceDateDesc(Pet pet);
    List<FacilityRecord> findByVeterinarianOrderByServiceDateDesc(User veterinarian);
}
