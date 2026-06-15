package com.example.library.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BorrowingRequest {
    @NotNull(message = "ID читателя обязателен")
    private Long readerId;

    @NotNull(message = "ID книги обязателен")
    private Long bookId;

    private LocalDateTime dueDate; // Если пустая, установим 14 дней по умолчанию в сервисе
}
