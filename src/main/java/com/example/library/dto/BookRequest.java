package com.example.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookRequest {
    @NotBlank(message = "Название книги не может быть пустым")
    private String title;

    @NotNull(message = "ID автора обязателен")
    private Long authorId;

    @NotBlank(message = "ISBN не может быть пустым")
    private String isbn;

    private LocalDate publishDate;

    @NotNull(message = "Количество обязательно")
    @Min(value = 0, message = "Количество не может быть меньше 0")
    private Integer quantity;
}
