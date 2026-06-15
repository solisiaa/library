package com.example.library.service;

import com.example.library.dto.ReaderRequest;
import com.example.library.dto.ReaderResponse;
import com.example.library.entity.Reader;
import com.example.library.entity.ReaderStatus;
import com.example.library.exception.BadRequestException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.ReaderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReaderService {

    private final ReaderRepository readerRepository;

    public ReaderService(ReaderRepository readerRepository) {
        this.readerRepository = readerRepository;
    }

    public List<ReaderResponse> getAllReaders() {
        return readerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ReaderResponse getReaderById(Long id) {
        Reader reader = readerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Читатель не найден с id: " + id));
        return mapToResponse(reader);
    }

    public ReaderResponse getReaderByUsername(String username) {
        Reader reader = readerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Читатель не найден для пользователя: " + username));
        return mapToResponse(reader);
    }

    @Transactional
    public ReaderResponse updateReader(Long id, ReaderRequest readerRequest) {
        Reader reader = readerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Читатель не найден с id: " + id));

        if (!reader.getEmail().equals(readerRequest.getEmail()) && readerRepository.existsByEmail(readerRequest.getEmail())) {
            throw new BadRequestException("Email уже используется другим пользователем: " + readerRequest.getEmail());
        }

        reader.setName(readerRequest.getName());
        reader.setEmail(readerRequest.getEmail());
        reader.setPhone(readerRequest.getPhone());

        if (readerRequest.getStatus() != null) {
            try {
                reader.setStatus(ReaderStatus.valueOf(readerRequest.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Недопустимый статус читателя: " + readerRequest.getStatus());
            }
        }

        if (reader.getUser() != null) {
            reader.getUser().setEmail(readerRequest.getEmail());
        }

        Reader updatedReader = readerRepository.save(reader);
        return mapToResponse(updatedReader);
    }

    @Transactional
    public void deleteReader(Long id) {
        if (!readerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Читатель не найден с id: " + id);
        }
        readerRepository.deleteById(id);
    }

    public ReaderResponse mapToResponse(Reader reader) {
        if (reader == null) return null;
        return ReaderResponse.builder()
                .id(reader.getId())
                .username(reader.getUser() != null ? reader.getUser().getUsername() : null)
                .name(reader.getName())
                .email(reader.getEmail())
                .phone(reader.getPhone())
                .status(reader.getStatus().name())
                .registrationDate(reader.getRegistrationDate())
                .build();
    }
}
