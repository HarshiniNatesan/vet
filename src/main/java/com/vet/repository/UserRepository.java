package com.vet.repository;
import com.vet.model.User;
import com.vet.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(Role role);
}
