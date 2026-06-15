package com.example.library.service;

import com.example.library.dto.JwtResponse;
import com.example.library.dto.LoginRequest;
import com.example.library.dto.RegisterRequest;
import com.example.library.entity.Reader;
import com.example.library.entity.ReaderStatus;
import com.example.library.entity.Role;
import com.example.library.entity.User;
import com.example.library.exception.BadRequestException;
import com.example.library.repository.ReaderRepository;
import com.example.library.repository.UserRepository;
import com.example.library.security.JwtUtils;
import com.example.library.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReaderRepository readerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .email("test@example.com")
                .role(Role.ROLE_READER)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("new@example.com");
        registerRequest.setName("New User");
        registerRequest.setPhone("1234567");
    }

    @Test
    void authenticateUser_Success() {
        Authentication auth = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("jwtToken");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        JwtResponse response = authService.authenticateUser(loginRequest);

        assertNotNull(response);
        assertEquals("jwtToken", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("ROLE_READER", response.getRole());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateUser_UserNotFound() {
        Authentication auth = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("jwtToken");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.authenticateUser(loginRequest));
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.registerUser(registerRequest));

        verify(userRepository).save(any(User.class));
        verify(readerRepository).save(any(Reader.class));
    }

    @Test
    void registerUser_UsernameExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.registerUser(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_EmailExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.registerUser(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_SavesUserWithCorrectRole() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(testUser);

        authService.registerUser(registerRequest);

        User savedUser = userCaptor.getValue();
        assertEquals(Role.ROLE_READER, savedUser.getRole());
        assertEquals("newuser", savedUser.getUsername());
    }

    @Test
    void registerUser_SavesReaderWithCorrectDetails() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        ArgumentCaptor<Reader> readerCaptor = ArgumentCaptor.forClass(Reader.class);
        when(readerRepository.save(readerCaptor.capture())).thenReturn(null);

        authService.registerUser(registerRequest);

        Reader savedReader = readerCaptor.getValue();
        assertEquals("New User", savedReader.getName());
        assertEquals("new@example.com", savedReader.getEmail());
        assertEquals("1234567", savedReader.getPhone());
        assertEquals(ReaderStatus.ACTIVE, savedReader.getStatus());
        assertNotNull(savedReader.getRegistrationDate());
    }

    @Test
    void registerUser_EncodesPassword() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.registerUser(registerRequest);

        verify(passwordEncoder).encode("password123");
    }
}
