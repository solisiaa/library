package com.example.library.service;

import com.example.library.dto.BookRequest;
import com.example.library.dto.BookResponse;
import com.example.library.entity.Author;
import com.example.library.entity.Book;
import com.example.library.exception.BadRequestException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final AuthorService authorService;

    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository,
                       AuthorService authorService) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.authorService = authorService;
    }

    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Книга не найдена с id: " + id));
        return mapToResponse(book);
    }

    @Transactional
    public BookResponse createBook(BookRequest bookRequest) {
        if (bookRepository.existsByIsbn(bookRequest.getIsbn())) {
            throw new BadRequestException("Книга с таким ISBN уже существует: " + bookRequest.getIsbn());
        }

        Author author = authorRepository.findById(bookRequest.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Автор не найден с id: " + bookRequest.getAuthorId()));

        Book book = Book.builder()
                .title(bookRequest.getTitle())
                .author(author)
                .isbn(bookRequest.getIsbn())
                .publishDate(bookRequest.getPublishDate())
                .quantity(bookRequest.getQuantity())
                .availableQuantity(bookRequest.getQuantity())
                .build();

        Book savedBook = bookRepository.save(book);
        return mapToResponse(savedBook);
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest bookRequest) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Книга не найдена с id: " + id));

        if (!book.getIsbn().equals(bookRequest.getIsbn()) && bookRepository.existsByIsbn(bookRequest.getIsbn())) {
            throw new BadRequestException("Книга с таким ISBN уже существует: " + bookRequest.getIsbn());
        }

        Author author = authorRepository.findById(bookRequest.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Автор не найден с id: " + bookRequest.getAuthorId()));

        int borrowedQuantity = book.getQuantity() - book.getAvailableQuantity();
        if (bookRequest.getQuantity() < borrowedQuantity) {
            throw new BadRequestException("Новое общее количество книг (" + bookRequest.getQuantity() + 
                    ") не может быть меньше количества выданных на руки книг (" + borrowedQuantity + ")");
        }

        book.setTitle(bookRequest.getTitle());
        book.setAuthor(author);
        book.setIsbn(bookRequest.getIsbn());
        book.setPublishDate(bookRequest.getPublishDate());
        book.setAvailableQuantity(bookRequest.getQuantity() - borrowedQuantity);
        book.setQuantity(bookRequest.getQuantity());

        Book updatedBook = bookRepository.save(book);
        return mapToResponse(updatedBook);
    }

    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Книга не найдена с id: " + id);
        }
        bookRepository.deleteById(id);
    }

    public BookResponse mapToResponse(Book book) {
        if (book == null) return null;
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(authorService.mapToResponse(book.getAuthor()))
                .isbn(book.getIsbn())
                .publishDate(book.getPublishDate())
                .quantity(book.getQuantity())
                .availableQuantity(book.getAvailableQuantity())
                .build();
    }
}
