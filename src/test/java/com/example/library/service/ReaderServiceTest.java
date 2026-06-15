package com.example.library.service;

import com.example.library.dto.ReaderRequest;
import com.example.library.dto.ReaderResponse;
import com.example.library.entity.Reader;
import com.example.library.entity.ReaderStatus;
import com.example.library.entity.User;
import com.example.library.exception.BadRequestException;
import com.example.library.exception.ResourceNotFoundException;
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
public class ReaderServiceTest {

    @Mock
    private ReaderRepository readerRepository;

    @InjectMocks
    private ReaderService readerService;

    private User testUser;
    private Reader testReader;
    private ReaderRequest readerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("reader1")
                .email("reader1@example.com")
                .build();

        testReader = Reader.builder()
                .id(1L)
                .user(testUser)
                .name("Иван Иванов")
                .email("reader1@example.com")
                .phone("12345")
                .status(ReaderStatus.ACTIVE)
                .registrationDate(LocalDateTime.now())
                .build();

        readerRequest = new ReaderRequest();
        readerRequest.setName("Иван Петров");
        readerRequest.setEmail("reader1@example.com");
        readerRequest.setPhone("54321");
        readerRequest.setStatus("BLOCKED");
    }

    @Test
    void getAllReaders_Success() {
        when(readerRepository.findAll()).thenReturn(Arrays.asList(testReader));

        List<ReaderResponse> result = readerService.getAllReaders();

        assertEquals(1, result.size());
        assertEquals("Иван Иванов", result.get(0).getName());
    }

    @Test
    void getAllReaders_Empty() {
        when(readerRepository.findAll()).thenReturn(Collections.emptyList());

        List<ReaderResponse> result = readerService.getAllReaders();

        assertTrue(result.isEmpty());
    }

    @Test
    void getReaderById_Success() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(testReader));

        ReaderResponse result = readerService.getReaderById(1L);

        assertNotNull(result);
        assertEquals("Иван Иванов", result.getName());
    }

    @Test
    void getReaderById_NotFound() {
        when(readerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> readerService.getReaderById(99L));
    }

    @Test
    void getReaderByUsername_Success() {
        when(readerRepository.findByUserUsername("reader1")).thenReturn(Optional.of(testReader));

        ReaderResponse result = readerService.getReaderByUsername("reader1");

        assertNotNull(result);
        assertEquals("Иван Иванов", result.getName());
    }

    @Test
    void getReaderByUsername_NotFound() {
        when(readerRepository.findByUserUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> readerService.getReaderByUsername("unknown"));
    }

    @Test
    void updateReader_Success() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(testReader));
        when(readerRepository.save(any(Reader.class))).thenReturn(testReader);

        ReaderResponse result = readerService.updateReader(1L, readerRequest);

        assertNotNull(result);
        assertEquals("Иван Петров", result.getName());
        assertEquals("BLOCKED", result.getStatus());
        assertEquals("reader1@example.com", testUser.getEmail()); // Синхронизация с User
    }

    @Test
    void updateReader_EmailExists() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(testReader));
        
        // Меняем email на новый, который уже занят
        readerRequest.setEmail("other@example.com");
        when(readerRepository.existsByEmail("other@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> readerService.updateReader(1L, readerRequest));
    }

    @Test
    void updateReader_NotFound() {
        when(readerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> readerService.updateReader(99L, readerRequest));
    }

    @Test
    void deleteReader_Success() {
        when(readerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(readerRepository).deleteById(1L);

        assertDoesNotThrow(() -> readerService.deleteReader(1L));

        verify(readerRepository).deleteById(1L);
    }

    @Test
    void deleteReader_NotFound() {
        when(readerRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> readerService.deleteReader(99L));
    }
}
