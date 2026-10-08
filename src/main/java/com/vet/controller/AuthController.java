package com.vet.controller;

import com.vet.model.User;
import com.vet.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserRepository users,
                          PasswordEncoder encoder,
                          SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.encoder = encoder;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");
        String requestedRole = body.get("role");

        if (username == null || username.trim().isEmpty() ||
                password == null || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username and password are required.");
        }

        User u = users.findByUsername(username.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (Boolean.FALSE.equals(u.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Account is deactivated.");
        }

        if (!encoder.matches(password, u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid username or password");
        }

        // Validate the selected role BEFORE creating the session.
        if (requestedRole != null && !requestedRole.trim().isEmpty()
                && !u.getRole().name().equals(requestedRole.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Selected role does not match this account.");
        }

        u.setLastLogin(LocalDateTime.now());
        users.save(u);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                u.getUsername(),
                null,
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + u.getRole().name())
                )
        );

        // Create the HTTP session that will hold the authenticated SecurityContext.
        request.getSession(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Login successful");
        result.put("username", u.getUsername());
        result.put("role", u.getRole().name());
        return result;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        User u = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User account not found"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", u.getUsername());
        result.put("role", u.getRole().name());
        result.put("fullName", u.getFullName());
        result.put("active", u.getActive());
        return result;
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return Collections.singletonMap("message", "Logged out");
    }
}
