package com.example.library.controller;

import com.example.library.dto.ReaderRequest;
import com.example.library.dto.ReaderResponse;
import com.example.library.exception.BadRequestException;
import com.example.library.service.ReaderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/readers")
public class ReaderController {

    private final ReaderService readerService;

    public ReaderController(ReaderService readerService) {
        this.readerService = readerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public List<ReaderResponse> getAllReaders() {
        return readerService.getAllReaders();
    }

    @GetMapping("/{id}")
    public ReaderResponse getReaderById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        ReaderResponse reader = readerService.getReaderById(id);
        boolean isAdminOrLibrarian = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
        
        if (!isAdminOrLibrarian && !userDetails.getUsername().equals(reader.getUsername())) {
            throw new BadRequestException("Вы не можете просматривать профиль другого читателя");
        }
        return reader;
    }

    @PutMapping("/{id}")
    public ReaderResponse updateReader(@PathVariable Long id, 
                                       @Valid @RequestBody ReaderRequest readerRequest, 
                                       @AuthenticationPrincipal UserDetails userDetails) {
        ReaderResponse reader = readerService.getReaderById(id);
        boolean isAdminOrLibrarian = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));

        if (!isAdminOrLibrarian && !userDetails.getUsername().equals(reader.getUsername())) {
            throw new BadRequestException("Вы не можете обновлять профиль другого читателя");
        }

        if (!isAdminOrLibrarian && readerRequest.getStatus() != null) {
            readerRequest.setStatus(null);
        }

        return readerService.updateReader(id, readerRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void deleteReader(@PathVariable Long id) {
        readerService.deleteReader(id);
    }
}
