package com.vet.config;

import com.vet.model.User;
import com.vet.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(UserRepository repo){
        return username -> {
            User u = repo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                    .password(u.getPassword())
                    .roles(u.getRole().name())
                    .build();
        };
    }

    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests()
            .antMatchers("/", "/index.html", "/login.html", "/css/**", "/js/**", "/api/auth/login").permitAll()
            .antMatchers("/api/owner/**").hasRole("PET_OWNER")
            .antMatchers("/api/vet/**").hasRole("VETERINARIAN")
            .antMatchers("/api/admin/**").hasRole("ADMIN")
            .antMatchers("/api/reception/**").hasRole("RECEPTIONIST")
            .antMatchers("/api/pharmacy/**").hasRole("PHARMACY")
            .anyRequest().authenticated()
            .and()
            .formLogin().disable()
            .httpBasic();
        return http.build();
    }
}
