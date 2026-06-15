package com.example.library.service;

import com.example.library.dto.AuthorRequest;
import com.example.library.dto.AuthorResponse;
import com.example.library.entity.Author;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    private Author author1;
    private Author author2;
    private AuthorRequest authorRequest;

    @BeforeEach
    void setUp() {
        author1 = Author.builder()
                .id(1L)
                .name("Лев Толстой")
                .biography("Великий писатель")
                .birthDate(LocalDate.of(1828, 9, 9))
                .build();

        author2 = Author.builder()
                .id(2L)
                .name("Федор Достоевский")
                .biography("Классик русской литературы")
                .birthDate(LocalDate.of(1821, 11, 11))
                .build();

        authorRequest = new AuthorRequest();
        authorRequest.setName("Александр Пушкин");
        authorRequest.setBiography("Солнце русской поэзии");
        authorRequest.setBirthDate(LocalDate.of(1799, 6, 6));
    }

    @Test
    void getAllAuthors_ReturnsList() {
        when(authorRepository.findAll()).thenReturn(Arrays.asList(author1, author2));

        List<AuthorResponse> result = authorService.getAllAuthors();

        assertEquals(2, result.size());
        assertEquals("Лев Толстой", result.get(0).getName());
        assertEquals("Федор Достоевский", result.get(1).getName());
    }

    @Test
    void getAllAuthors_ReturnsEmptyList() {
        when(authorRepository.findAll()).thenReturn(Collections.emptyList());

        List<AuthorResponse> result = authorService.getAllAuthors();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAuthorById_Success() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author1));

        AuthorResponse result = authorService.getAuthorById(1L);

        assertNotNull(result);
        assertEquals("Лев Толстой", result.getName());
    }

    @Test
    void getAuthorById_NotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authorService.getAuthorById(99L));
    }

    @Test
    void createAuthor_Success() {
        Author savedAuthor = Author.builder()
                .id(3L)
                .name(authorRequest.getName())
                .biography(authorRequest.getBiography())
                .birthDate(authorRequest.getBirthDate())
                .build();
        when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);

        AuthorResponse result = authorService.createAuthor(authorRequest);

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Александр Пушкин", result.getName());
    }

    @Test
    void updateAuthor_Success() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author1));
        when(authorRepository.save(any(Author.class))).thenReturn(author1);

        AuthorResponse result = authorService.updateAuthor(1L, authorRequest);

        assertNotNull(result);
        assertEquals("Александр Пушкин", result.getName());
        assertEquals("Солнце русской поэзии", result.getBiography());
    }

    @Test
    void updateAuthor_NotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authorService.updateAuthor(99L, authorRequest));
    }

    @Test
    void deleteAuthor_Success() {
        when(authorRepository.existsById(1L)).thenReturn(true);
        doNothing().when(authorRepository).deleteById(1L);

        assertDoesNotThrow(() -> authorService.deleteAuthor(1L));

        verify(authorRepository).deleteById(1L);
    }

    @Test
    void deleteAuthor_NotFound() {
        when(authorRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authorService.deleteAuthor(99L));
        verify(authorRepository, never()).deleteById(anyLong());
    }
}
