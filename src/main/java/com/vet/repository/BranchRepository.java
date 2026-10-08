package com.vet.repository;
import com.vet.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BranchRepository extends JpaRepository<Branch,Long> {
    Optional<Branch> findByBranchCodeIgnoreCase(String branchCode);
    List<Branch> findAllByOrderByBranchNameAsc();
}
