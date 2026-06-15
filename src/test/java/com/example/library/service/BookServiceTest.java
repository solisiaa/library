package com.example.library.service;

import com.example.library.dto.AuthorResponse;
import com.example.library.dto.BookRequest;
import com.example.library.dto.BookResponse;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.exception.BadRequestException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
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
public class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private AuthorService authorService;

    @InjectMocks
    private BookService bookService;

    private Author testAuthor;
    private Book testBook;
    private BookRequest bookRequest;

    @BeforeEach
    void setUp() {
        testAuthor = Author.builder()
                .id(1L)
                .name("Александр Пушкин")
                .build();

        testBook = Book.builder()
                .id(1L)
                .title("Евгений Онегин")
                .author(testAuthor)
                .isbn("123-456-789")
                .publishDate(LocalDate.of(1833, 1, 1))
                .quantity(10)
                .availableQuantity(8)
                .build();

        bookRequest = new BookRequest();
        bookRequest.setTitle("Евгений Онегин");
        bookRequest.setAuthorId(1L);
        bookRequest.setIsbn("123-456-789");
        bookRequest.setPublishDate(LocalDate.of(1833, 1, 1));
        bookRequest.setQuantity(10);
    }

    @Test
    void getAllBooks_Success() {
        when(bookRepository.findAll()).thenReturn(Arrays.asList(testBook));
        when(authorService.mapToResponse(testAuthor)).thenReturn(new AuthorResponse(1L, "Александр Пушкин", null, null));

        List<BookResponse> result = bookService.getAllBooks();

        assertEquals(1, result.size());
        assertEquals("Евгений Онегин", result.get(0).getTitle());
    }

    @Test
    void getAllBooks_Empty() {
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());

        List<BookResponse> result = bookService.getAllBooks();

        assertTrue(result.isEmpty());
    }

    @Test
    void getBookById_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(authorService.mapToResponse(testAuthor)).thenReturn(new AuthorResponse(1L, "Александр Пушкин", null, null));

        BookResponse result = bookService.getBookById(1L);

        assertNotNull(result);
        assertEquals("Евгений Онегин", result.getTitle());
    }

    @Test
    void getBookById_NotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(99L));
    }

    @Test
    void createBook_Success() {
        when(bookRepository.existsByIsbn("123-456-789")).thenReturn(false);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        when(authorService.mapToResponse(testAuthor)).thenReturn(new AuthorResponse(1L, "Александр Пушкин", null, null));

        BookResponse result = bookService.createBook(bookRequest);

        assertNotNull(result);
        assertEquals("Евгений Онегин", result.getTitle());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void createBook_IsbnExists() {
        when(bookRepository.existsByIsbn("123-456-789")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> bookService.createBook(bookRequest));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void createBook_AuthorNotFound() {
        when(bookRepository.existsByIsbn("123-456-789")).thenReturn(false);
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.createBook(bookRequest));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void updateBook_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        BookResponse result = bookService.updateBook(1L, bookRequest);

        assertNotNull(result);
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void updateBook_IsbnExistsForDifferentBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        bookRequest.setIsbn("999-999-999");
        when(bookRepository.existsByIsbn("999-999-999")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> bookService.updateBook(1L, bookRequest));
    }

    @Test
    void updateBook_AuthorNotFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.updateBook(1L, bookRequest));
    }

    @Test
    void updateBook_NewQuantityLessThanBorrowed() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook)); // quantity = 10, available = 8 => borrowed = 2
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        
        // Попытка поставить общее количество = 1 (меньше выданных 2-х)
        bookRequest.setQuantity(1);

        assertThrows(BadRequestException.class, () -> bookService.updateBook(1L, bookRequest));
    }

    @Test
    void deleteBook_Success() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        doNothing().when(bookRepository).deleteById(1L);

        assertDoesNotThrow(() -> bookService.deleteBook(1L));

        verify(bookRepository).deleteById(1L);
    }

    @Test
    void deleteBook_NotFound() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> bookService.deleteBook(99L));
    }
}
