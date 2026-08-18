package com.vet.controller;

import com.vet.model.User;
import com.vet.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users; private final PasswordEncoder encoder;
    public AuthController(UserRepository users, PasswordEncoder encoder){this.users=users;this.encoder=encoder;}

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,String> body) {
        User u=users.findByUsername(body.get("username")).orElseThrow(()->new RuntimeException("Invalid username or password"));
        if(!encoder.matches(body.get("password"),u.getPassword())) throw new RuntimeException("Invalid username or password");
        Map<String,Object> r=new HashMap<>(); r.put("message","Login successful"); r.put("username",u.getUsername()); r.put("role",u.getRole()); return r;
    }
}
