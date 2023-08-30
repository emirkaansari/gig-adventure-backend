package com.gigadventure.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigadventure.backend.api.controller.AuthController;
import com.gigadventure.backend.api.dto.AuthResponseDto;
import com.gigadventure.backend.api.dto.LoginDto;
import com.gigadventure.backend.api.dto.RegisterDto;
import com.gigadventure.backend.api.models.Role;
import com.gigadventure.backend.api.models.UserEntity;
import com.gigadventure.backend.api.repository.RoleRepository;
import com.gigadventure.backend.api.repository.UserRepository;
import com.gigadventure.backend.api.security.JwtAuthenticationFilter;
import com.gigadventure.backend.api.security.JwtGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    private AuthController authController;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtGenerator jwtGenerator;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        authController = new AuthController(
                authenticationManager,
                userRepository,
                roleRepository,
                passwordEncoder,
                jwtGenerator,
                jwtAuthenticationFilter
        );
    }

    @Test
    public void testRegister() {
        RegisterDto registerDto = new RegisterDto();
        registerDto.setUsername("testuser");
        registerDto.setPassword("testpassword");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);

        Role role = new Role();
        role.setName("USER");
        when(roleRepository.findByName("USER")).thenReturn(java.util.Optional.of(role));

        ResponseEntity<String> response = authController.register(registerDto);

        verify(userRepository, times(1)).save(any(UserEntity.class));
        verify(passwordEncoder, times(1)).encode("testpassword");

        assertSame(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testLogin() {
        MockitoAnnotations.initMocks(this);

        // Create a mock for SecurityContext
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);

        // Create a mock for Authentication
        Authentication authentication = Mockito.mock(Authentication.class);

        // Set up the mock behavior for authenticationManager
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Set the SecurityContext to your desired behavior
        SecurityContextHolder.setContext(securityContext);

        // Rest of your test logic
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("testuser");
        loginDto.setPassword("testpassword");

        when(jwtGenerator.generateToken(authentication)).thenReturn("generated_token");

        AuthController authController = new AuthController(
                authenticationManager,
                userRepository,
                roleRepository,
                passwordEncoder,
                jwtGenerator,
                jwtAuthenticationFilter
        );

        // Call the login method
        ResponseEntity<AuthResponseDto> response = authController.login(loginDto);

        // Verify interactions on the SecurityContext mock
        Mockito.verify(securityContext, Mockito.times(1)).setAuthentication(authentication);

        // Assertions
        assertSame(HttpStatus.OK, response.getStatusCode());
        assertEquals("generated_token", response.getBody().getAccessToken());
    }

    @Test
    public void testLogout() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(jwtAuthenticationFilter.getJwtFromRequest(request)).thenReturn("test_token");

        ResponseEntity<String> response = authController.logout(request);

        verify(jwtGenerator, times(1)).invalidateJwtToken("test_token");

        assertSame(HttpStatus.OK, response.getStatusCode());
    }
}
