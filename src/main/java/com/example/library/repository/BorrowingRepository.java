package com.example.library.repository;

import com.example.library.entity.Borrowing;
import com.example.library.entity.BorrowingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
    List<Borrowing> findByReaderId(Long readerId);
    List<Borrowing> findByReaderUserUsername(String username);
    List<Borrowing> findByStatus(BorrowingStatus status);
    List<Borrowing> findByStatusAndDueDateBefore(BorrowingStatus status, LocalDateTime now);
}
