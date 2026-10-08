package com.vet.repository;
import com.vet.model.AdminActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog,Long>{
    List<AdminActivityLog> findTop100ByOrderByEventTimeDesc();
}
