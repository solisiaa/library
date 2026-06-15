package com.example.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReaderResponse {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String status;
    private LocalDateTime registrationDate;
}
