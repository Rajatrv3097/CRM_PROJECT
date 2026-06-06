package com.softcrm.service;

import com.softcrm.entity.user.User;
import com.softcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("🔍 Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        log.info("✅ User found: {} - Type: {} - Active: {}", user.getEmail(), user.getUserType(), user.isActive());

        if (!user.isActive()) {
            log.warn("⚠️ User account is inactive: {}", email);
            throw new UsernameNotFoundException("User account is inactive");
        }

        // Build authorities - IMPORTANT: Add ROLE_ prefix for hasRole() to work
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 1. Add user type as ROLE (for @PreAuthorize("hasRole('XXX')"))
        String roleName = user.getUserType();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        log.info("📌 Added role authority: ROLE_{}", roleName);

        // 2. Add all permissions from roles (for @PreAuthorize("hasAuthority('XXX')"))
        user.getRoles().forEach(role -> {
            role.getPermissions().forEach(permission -> {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
                log.debug("   Added permission authority: {}", permission.getName());
            });
        });

        log.info("🎉 User loaded successfully with {} authorities", authorities.size());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                authorities
        );
    }
}