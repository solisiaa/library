package com.example.library.controller;

import com.example.library.dto.BorrowingRequest;
import com.example.library.dto.BorrowingResponse;
import com.example.library.exception.BadRequestException;
import com.example.library.service.BorrowingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrowings")
public class BorrowingController {

    private final BorrowingService borrowingService;

    public BorrowingController(BorrowingService borrowingService) {
        this.borrowingService = borrowingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public List<BorrowingResponse> getAllBorrowings() {
        return borrowingService.getAllBorrowings();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_READER')")
    public List<BorrowingResponse> getMyBorrowings(@AuthenticationPrincipal UserDetails userDetails) {
        return borrowingService.getMyBorrowings(userDetails.getUsername());
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public List<BorrowingResponse> getOverdueBorrowings() {
        return borrowingService.getOverdueBorrowings();
    }

    @GetMapping("/{id}")
    public BorrowingResponse getBorrowingById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        BorrowingResponse borrowing = borrowingService.getBorrowingById(id);
        boolean isAdminOrLibrarian = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));

        if (!isAdminOrLibrarian && !userDetails.getUsername().equals(borrowing.getReaderUsername())) {
            throw new BadRequestException("Вы не можете просматривать чужую запись о выдаче книги");
        }
        return borrowing;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public BorrowingResponse borrowBook(@Valid @RequestBody BorrowingRequest request) {
        return borrowingService.borrowBook(request);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public BorrowingResponse returnBook(@PathVariable Long id) {
        return borrowingService.returnBook(id);
    }
}
