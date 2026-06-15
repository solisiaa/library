package com.example.library.service;

import com.example.library.dto.BorrowingRequest;
import com.example.library.dto.BorrowingResponse;
import com.example.library.entity.*;
import com.example.library.exception.BadRequestException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowingRepository;
import com.example.library.repository.ReaderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowingService {

    private final BorrowingRepository borrowingRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;

    public BorrowingService(BorrowingRepository borrowingRepository,
                            BookRepository bookRepository,
                            ReaderRepository readerRepository) {
        this.borrowingRepository = borrowingRepository;
        this.bookRepository = bookRepository;
        this.readerRepository = readerRepository;
    }

    public List<BorrowingResponse> getAllBorrowings() {
        checkAndUpdateOverdueStatus();
        return borrowingRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BorrowingResponse> getMyBorrowings(String username) {
        checkAndUpdateOverdueStatus();
        return borrowingRepository.findByReaderUserUsername(username).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BorrowingResponse getBorrowingById(Long id) {
        Borrowing borrowing = borrowingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись о выдаче не найдена с id: " + id));
        
        if (borrowing.getStatus() == BorrowingStatus.BORROWED && borrowing.getDueDate().isBefore(LocalDateTime.now())) {
            borrowing.setStatus(BorrowingStatus.OVERDUE);
            borrowing = borrowingRepository.save(borrowing);
        }
        
        return mapToResponse(borrowing);
    }

    @Transactional
    public BorrowingResponse borrowBook(BorrowingRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Книга не найдена с id: " + request.getBookId()));

        Reader reader = readerRepository.findById(request.getReaderId())
                .orElseThrow(() -> new ResourceNotFoundException("Читатель не найден с id: " + request.getReaderId()));

        if (reader.getStatus() == ReaderStatus.BLOCKED) {
            throw new BadRequestException("Заблокированный читатель не может брать книги!");
        }

        if (book.getAvailableQuantity() <= 0) {
            throw new BadRequestException("Нет доступных экземпляров книги '" + book.getTitle() + "' для выдачи!");
        }

        book.setAvailableQuantity(book.getAvailableQuantity() - 1);
        bookRepository.save(book);

        LocalDateTime dueDate = request.getDueDate() != null ? request.getDueDate() : LocalDateTime.now().plusDays(14);

        Borrowing borrowing = Borrowing.builder()
                .book(book)
                .reader(reader)
                .borrowDate(LocalDateTime.now())
                .dueDate(dueDate)
                .status(BorrowingStatus.BORROWED)
                .build();

        Borrowing savedBorrowing = borrowingRepository.save(borrowing);
        return mapToResponse(savedBorrowing);
    }

    @Transactional
    public BorrowingResponse returnBook(Long id) {
        Borrowing borrowing = borrowingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись о выдаче не найдена с id: " + id));

        if (borrowing.getStatus() == BorrowingStatus.RETURNED) {
            throw new BadRequestException("Книга уже возвращена по этой выдаче!");
        }

        Book book = borrowing.getBook();
        book.setAvailableQuantity(book.getAvailableQuantity() + 1);
        bookRepository.save(book);

        borrowing.setReturnDate(LocalDateTime.now());
        borrowing.setStatus(BorrowingStatus.RETURNED);

        Borrowing savedBorrowing = borrowingRepository.save(borrowing);
        return mapToResponse(savedBorrowing);
    }

    public List<BorrowingResponse> getOverdueBorrowings() {
        checkAndUpdateOverdueStatus();
        return borrowingRepository.findByStatus(BorrowingStatus.OVERDUE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void checkAndUpdateOverdueStatus() {
        List<Borrowing> activeBorrowings = borrowingRepository.findByStatus(BorrowingStatus.BORROWED);
        LocalDateTime now = LocalDateTime.now();
        for (Borrowing b : activeBorrowings) {
            if (b.getDueDate().isBefore(now)) {
                b.setStatus(BorrowingStatus.OVERDUE);
                borrowingRepository.save(b);
            }
        }
    }

    private BorrowingResponse mapToResponse(Borrowing borrowing) {
        if (borrowing == null) return null;
        return BorrowingResponse.builder()
                .id(borrowing.getId())
                .readerId(borrowing.getReader().getId())
                .readerName(borrowing.getReader().getName())
                .bookId(borrowing.getBook().getId())
                .bookTitle(borrowing.getBook().getTitle())
                .borrowDate(borrowing.getBorrowDate())
                .returnDate(borrowing.getReturnDate())
                .dueDate(borrowing.getDueDate())
                .status(borrowing.getStatus().name())
                .build();
    }
}
