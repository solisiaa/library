package com.example.library.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final JwtUtils jwtUtils;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          AuthEntryPointJwt unauthorizedHandler,
                          JwtUtils jwtUtils) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtUtils = jwtUtils;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                auth.requestMatchers("/api/auth/**").permitAll()
                    // Книги и Авторы: просмотр доступен всем авторизованным пользователям
                    .requestMatchers(HttpMethod.GET, "/api/books/**", "/api/authors/**").hasAnyAuthority("ROLE_READER", "ROLE_LIBRARIAN", "ROLE_ADMIN")
                    // Книги и Авторы: добавление/изменение доступно только LIBRARIAN и ADMIN
                    .requestMatchers(HttpMethod.POST, "/api/books/**", "/api/authors/**").hasAnyAuthority("ROLE_LIBRARIAN", "ROLE_ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/books/**", "/api/authors/**").hasAnyAuthority("ROLE_LIBRARIAN", "ROLE_ADMIN")
                    // Книги и Авторы: удаление доступно только ADMIN
                    .requestMatchers(HttpMethod.DELETE, "/api/books/**", "/api/authors/**").hasAuthority("ROLE_ADMIN")
                    // Читатели: просмотр и изменение доступно LIBRARIAN, ADMIN, READER
                    .requestMatchers("/api/readers/**").hasAnyAuthority("ROLE_LIBRARIAN", "ROLE_ADMIN", "ROLE_READER")
                    // Выдачи: доступно LIBRARIAN, ADMIN, READER
                    .requestMatchers("/api/borrowings/**").hasAnyAuthority("ROLE_LIBRARIAN", "ROLE_ADMIN", "ROLE_READER")
                    .anyRequest().authenticated()
            );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
