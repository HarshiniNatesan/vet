package com.vet.controller;

import com.vet.model.User;
import com.vet.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserRepository users, PasswordEncoder encoder){this.users=users;this.encoder=encoder;}

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,String> body, HttpServletRequest request, HttpServletResponse response) {
        User u=users.findByUsername(body.get("username")).orElseThrow(()->new RuntimeException("Invalid username or password"));
        if(!encoder.matches(body.get("password"),u.getPassword())) throw new RuntimeException("Invalid username or password");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                u.getUsername(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        Map<String,Object> r=new HashMap<>();
        r.put("message","Login successful");
        r.put("username",u.getUsername());
        r.put("role",u.getRole());
        return r;
    }

    @PostMapping("/logout")
    public Map<String,String> logout(HttpServletRequest request){
        SecurityContextHolder.clearContext();
        if(request.getSession(false)!=null) request.getSession(false).invalidate();
        return Collections.singletonMap("message","Logged out");
    }
}
