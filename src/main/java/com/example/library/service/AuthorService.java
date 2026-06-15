package com.example.library.service;

import com.example.library.dto.AuthorRequest;
import com.example.library.dto.AuthorResponse;
import com.example.library.entity.Author;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<AuthorResponse> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AuthorResponse getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Автор не найден с id: " + id));
        return mapToResponse(author);
    }

    @Transactional
    public AuthorResponse createAuthor(AuthorRequest authorRequest) {
        Author author = Author.builder()
                .name(authorRequest.getName())
                .biography(authorRequest.getBiography())
                .birthDate(authorRequest.getBirthDate())
                .build();
        Author savedAuthor = authorRepository.save(author);
        return mapToResponse(savedAuthor);
    }

    @Transactional
    public AuthorResponse updateAuthor(Long id, AuthorRequest authorRequest) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Автор не найден с id: " + id));

        author.setName(authorRequest.getName());
        author.setBiography(authorRequest.getBiography());
        author.setBirthDate(authorRequest.getBirthDate());

        Author updatedAuthor = authorRepository.save(author);
        return mapToResponse(updatedAuthor);
    }

    @Transactional
    public void deleteAuthor(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Автор не найден с id: " + id);
        }
        authorRepository.deleteById(id);
    }

    public AuthorResponse mapToResponse(Author author) {
        if (author == null) return null;
        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .biography(author.getBiography())
                .birthDate(author.getBirthDate())
                .build();
    }
}
