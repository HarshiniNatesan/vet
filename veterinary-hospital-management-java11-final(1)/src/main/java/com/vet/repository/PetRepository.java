package com.vet.repository;
import com.vet.model.Pet;
import com.vet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PetRepository extends JpaRepository<Pet,Long> {
    List<Pet> findByOwner(User owner);
}
