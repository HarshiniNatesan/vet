package com.vet.config;

import com.vet.model.User;
import com.vet.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository repo) {
        return username -> {
            User u = repo.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (Boolean.FALSE.equals(u.getActive())) {
                throw new org.springframework.security.authentication.DisabledException("Account is deactivated");
            }

            return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                    .password(u.getPassword())
                    .roles(u.getRole().name())
                    .build();
        };
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository())
                .requireExplicitSave(false)
            )
            .authorizeRequests(auth -> auth
                // Public application pages and static assets.
                // The actual data/API endpoints below remain role protected.
                .antMatchers(
                    "/", "/index.html", "/login.html", "/role.html",
                    "/admin.html", "/pharmacy.html", "/vet.html",
                    "/owner.html", "/reception.html",
                    "/css/**", "/js/**", "/images/**", "/favicon.ico",
                    "/api/auth/login",
                    "/public/pet-report/**",
                    "/pet-report-view.html",
                    "/api/payments/webhook"
                ).permitAll()
                .antMatchers("/api/auth/logout", "/api/auth/me").authenticated()
                .antMatchers("/api/owner/**").hasRole("PET_OWNER")
                .antMatchers("/api/vet/**").hasRole("VETERINARIAN")
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/reception/**").hasRole("RECEPTIONIST")
                .antMatchers("/api/pharmacy/**").hasRole("PHARMACY")
                .anyRequest().authenticated()
            )
            .formLogin().disable()
            .httpBasic().disable()
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
