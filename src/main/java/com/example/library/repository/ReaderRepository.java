package com.example.library.repository;

import com.example.library.entity.Reader;
import com.example.library.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReaderRepository extends JpaRepository<Reader, Long> {
    Optional<Reader> findByUser(User user);
    Optional<Reader> findByUserUsername(String username);
    Optional<Reader> findByEmail(String email);
    boolean existsByEmail(String email);
}
