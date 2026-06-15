package com.example.library.config;

import com.example.library.entity.Role;
import com.example.library.entity.User;
import com.example.library.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@library.com")
                    .role(Role.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("librarian")) {
            User librarian = User.builder()
                    .username("librarian")
                    .password(passwordEncoder.encode("lib123"))
                    .email("librarian@library.com")
                    .role(Role.ROLE_LIBRARIAN)
                    .build();
            userRepository.save(librarian);
        }
    }
}
