package com.example.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponse {
    private Long id;
    private String title;
    private AuthorResponse author;
    private String isbn;
    private LocalDate publishDate;
    private Integer quantity;
    private Integer availableQuantity;
}
