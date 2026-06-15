package com.example.library.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AuthorRequest {
    @NotBlank(message = "Имя автора не может быть пустым")
    private String name;

    private String biography;

    private LocalDate birthDate;
}
