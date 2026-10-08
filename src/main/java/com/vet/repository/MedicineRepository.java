package com.vet.repository;
import com.vet.model.Medicine;
import com.vet.model.Branch;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface MedicineRepository extends JpaRepository<Medicine,Long>{
    List<Medicine> findByActiveTrueOrderByNameAsc();
    List<Medicine> findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(String name);
    @Query("select m from Medicine m where m.active=true and m.quantity <= m.reorderLevel order by m.quantity asc") List<Medicine> findLowStock();
    Optional<Medicine> findFirstByNameIgnoreCaseAndActiveTrue(String name);
    List<Medicine> findByBranchAndActiveTrueOrderByNameAsc(Branch branch);
    List<Medicine> findAllByNameIgnoreCaseAndActiveTrue(String name);
}
