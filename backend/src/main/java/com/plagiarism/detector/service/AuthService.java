package com.plagiarism.detector.service;

import com.plagiarism.detector.model.AuthResponse;
import com.plagiarism.detector.model.LoginRequest;
import com.plagiarism.detector.model.RegisterRequest;
import com.plagiarism.detector.model.User;
import com.plagiarism.detector.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    
    public AuthService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder, 
                      JwtService jwtService, 
                      @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return user;
    }
    
    public AuthResponse register(RegisterRequest request) {
        // Check if passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return new AuthResponse("Passwords do not match", false);
        }
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            return new AuthResponse("Username already exists", false);
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse("Email already exists", false);
        }
        
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.UserRole.USER);
        
        userRepository.save(user);
        
        // Generate token
        String token = jwtService.generateToken(user);
        
        return new AuthResponse(token, user);
    }
    
    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            // Load user details
            UserDetails userDetails = loadUserByUsername(request.getUsername());
            
            // Generate token
            String token = jwtService.generateToken(userDetails);
            
            // Get user info
            User user = userRepository.findByUsername(request.getUsername()).orElse(null);
            
            return new AuthResponse(token, user);
            
        } catch (Exception e) {
            return new AuthResponse("Invalid username or password", false);
        }
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
} 