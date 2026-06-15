package com.example.library.service;

import com.example.library.dto.BorrowingRequest;
import com.example.library.dto.BorrowingResponse;
import com.example.library.entity.*;
import com.example.library.exception.BadRequestException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowingRepository;
import com.example.library.repository.ReaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BorrowingServiceTest {

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReaderRepository readerRepository;

    @InjectMocks
    private BorrowingService borrowingService;

    private Reader testReader;
    private Book testBook;
    private Borrowing testBorrowing;
    private BorrowingRequest borrowingRequest;

    @BeforeEach
    void setUp() {
        testReader = Reader.builder()
                .id(1L)
                .name("Иван Иванов")
                .status(ReaderStatus.ACTIVE)
                .build();

        testBook = Book.builder()
                .id(1L)
                .title("Евгений Онегин")
                .quantity(5)
                .availableQuantity(3)
                .build();

        testBorrowing = Borrowing.builder()
                .id(1L)
                .reader(testReader)
                .book(testBook)
                .borrowDate(LocalDateTime.now().minusDays(5))
                .dueDate(LocalDateTime.now().plusDays(9))
                .status(BorrowingStatus.BORROWED)
                .build();

        borrowingRequest = new BorrowingRequest();
        borrowingRequest.setBookId(1L);
        borrowingRequest.setReaderId(1L);
    }

    @Test
    void getAllBorrowings_Success() {
        when(borrowingRepository.findAll()).thenReturn(Arrays.asList(testBorrowing));
        List<BorrowingResponse> result = borrowingService.getAllBorrowings();
        assertEquals(1, result.size());
        assertEquals("Евгений Онегин", result.get(0).getBookTitle());
    }

    @Test
    void getMyBorrowings_Success() {
        when(borrowingRepository.findByReaderUserUsername("reader1")).thenReturn(Arrays.asList(testBorrowing));
        List<BorrowingResponse> result = borrowingService.getMyBorrowings("reader1");
        assertEquals(1, result.size());
    }

    @Test
    void getBorrowingById_Success() {
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(testBorrowing));
        BorrowingResponse result = borrowingService.getBorrowingById(1L);
        assertNotNull(result);
        assertEquals(BorrowingStatus.BORROWED.name(), result.getStatus());
    }

    @Test
    void getBorrowingById_NotFound() {
        when(borrowingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> borrowingService.getBorrowingById(99L));
    }

    @Test
    void getBorrowingById_UpdatesToOverdue() {
        testBorrowing.setDueDate(LocalDateTime.now().minusDays(1)); // Просрочена на 1 день
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(testBorrowing));
        when(borrowingRepository.save(any(Borrowing.class))).thenReturn(testBorrowing);

        BorrowingResponse result = borrowingService.getBorrowingById(1L);

        assertEquals(BorrowingStatus.OVERDUE.name(), result.getStatus());
        verify(borrowingRepository).save(testBorrowing);
    }

    @Test
    void borrowBook_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(readerRepository.findById(1L)).thenReturn(Optional.of(testReader));
        when(borrowingRepository.save(any(Borrowing.class))).thenReturn(testBorrowing);

        int initialAvailable = testBook.getAvailableQuantity();

        BorrowingResponse result = borrowingService.borrowBook(borrowingRequest);

        assertNotNull(result);
        assertEquals(initialAvailable - 1, testBook.getAvailableQuantity());
        verify(bookRepository).save(testBook);
        verify(borrowingRepository).save(any(Borrowing.class));
    }

    @Test
    void borrowBook_BookNotFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> borrowingService.borrowBook(borrowingRequest));
    }

    @Test
    void borrowBook_ReaderNotFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(readerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> borrowingService.borrowBook(borrowingRequest));
    }

    @Test
    void borrowBook_ReaderBlocked() {
        testReader.setStatus(ReaderStatus.BLOCKED);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(readerRepository.findById(1L)).thenReturn(Optional.of(testReader));

        assertThrows(BadRequestException.class, () -> borrowingService.borrowBook(borrowingRequest));
    }

    @Test
    void borrowBook_BookNotAvailable() {
        testBook.setAvailableQuantity(0);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(readerRepository.findById(1L)).thenReturn(Optional.of(testReader));

        assertThrows(BadRequestException.class, () -> borrowingService.borrowBook(borrowingRequest));
    }

    @Test
    void returnBook_Success() {
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(testBorrowing));
        when(borrowingRepository.save(any(Borrowing.class))).thenReturn(testBorrowing);

        int initialAvailable = testBook.getAvailableQuantity();

        BorrowingResponse result = borrowingService.returnBook(1L);

        assertNotNull(result);
        assertEquals(initialAvailable + 1, testBook.getAvailableQuantity());
        assertEquals(BorrowingStatus.RETURNED.name(), result.getStatus());
        assertNotNull(testBorrowing.getReturnDate());
        verify(bookRepository).save(testBook);
        verify(borrowingRepository).save(testBorrowing);
    }

    @Test
    void returnBook_NotFound() {
        when(borrowingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> borrowingService.returnBook(99L));
    }

    @Test
    void returnBook_AlreadyReturned() {
        testBorrowing.setStatus(BorrowingStatus.RETURNED);
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(testBorrowing));

        assertThrows(BadRequestException.class, () -> borrowingService.returnBook(1L));
    }

    @Test
    void getOverdueBorrowings_Success() {
        when(borrowingRepository.findByStatus(BorrowingStatus.OVERDUE)).thenReturn(Arrays.asList(testBorrowing));
        List<BorrowingResponse> result = borrowingService.getOverdueBorrowings();
        assertEquals(1, result.size());
    }

    @Test
    void checkAndUpdateOverdueStatus_UpdatesStatus() {
        testBorrowing.setDueDate(LocalDateTime.now().minusDays(1)); // Просрочена
        when(borrowingRepository.findByStatus(BorrowingStatus.BORROWED)).thenReturn(Arrays.asList(testBorrowing));

        borrowingService.checkAndUpdateOverdueStatus();

        assertEquals(BorrowingStatus.OVERDUE, testBorrowing.getStatus());
        verify(borrowingRepository).save(testBorrowing);
    }
}
