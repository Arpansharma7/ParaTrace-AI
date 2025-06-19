package com.plagiarism.detector.controller;

import com.plagiarism.detector.model.*;
import com.plagiarism.detector.repository.SearchHistoryRepository;
import com.plagiarism.detector.repository.SimilaritySourceRepository;
import com.plagiarism.detector.service.AIService;
import com.plagiarism.detector.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AIService aiService;
    private final AuthService authService;
    private final SearchHistoryRepository searchHistoryRepository;
    private final SimilaritySourceRepository similaritySourceRepository;

    @Autowired
    public AnalysisController(
            AIService aiService,
            AuthService authService,
            SearchHistoryRepository searchHistoryRepository,
            SimilaritySourceRepository similaritySourceRepository) {
        this.aiService = aiService;
        this.authService = authService;
        this.searchHistoryRepository = searchHistoryRepository;
        this.similaritySourceRepository = similaritySourceRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyzeText(@RequestBody AnalysisRequest request) {
        System.out.println("Received analysis request at /api/analyze");
        System.out.println("Request text length: " + (request.getText() != null ? request.getText().length() : 0));
        
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = null;
            
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                user = authService.getUserByUsername(authentication.getName());
            }
            
            if (user == null) {
                AnalysisResponse errorResponse = new AnalysisResponse();
                errorResponse.setError("Authentication required");
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            AnalysisResponse response = aiService.analyzeText(request, user);
            System.out.println("Analysis completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in controller: " + e.getMessage());
            e.printStackTrace();
            AnalysisResponse errorResponse = new AnalysisResponse();
            errorResponse.setError("Server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getSearchHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = null;
            
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                user = authService.getUserByUsername(authentication.getName());
            }
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<SearchHistory> historyPage = searchHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", historyPage.getContent());
            response.put("totalElements", historyPage.getTotalElements());
            response.put("totalPages", historyPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = null;
            
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                user = authService.getUserByUsername(authentication.getName());
            }
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            
            Map<String, Object> statistics = new HashMap<>();
            
            // Total searches
            long totalSearches = searchHistoryRepository.countByUser(user);
            statistics.put("totalSearches", totalSearches);
            
            // Average similarity score
            Double avgSimilarity = searchHistoryRepository.findAverageSimilarityScoreByUser(user);
            statistics.put("averageSimilarity", avgSimilarity != null ? avgSimilarity : 0.0);
            
            // Recent searches (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            List<SearchHistory> recentSearches = searchHistoryRepository
                .findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, weekAgo, LocalDateTime.now());
            statistics.put("recentSearches", recentSearches.size());
            
            // High similarity searches (>50%)
            List<SearchHistory> highSimilaritySearches = searchHistoryRepository
                .findByUserAndSimilarityScoreGreaterThanOrderBySimilarityScoreDesc(user, 50.0);
            statistics.put("highSimilaritySearches", highSimilaritySearches.size());
            
            // Failed searches
            List<SearchHistory> failedSearches = searchHistoryRepository
                .findByUserAndStatusOrderByCreatedAtDesc(user, SearchHistory.SearchStatus.FAILED);
            statistics.put("failedSearches", failedSearches.size());
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/history/{id}/sources")
    public ResponseEntity<List<SimilaritySource>> getSearchSources(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = null;
            
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                user = authService.getUserByUsername(authentication.getName());
            }
            
            if (user == null) {
                return ResponseEntity.status(401).build();
            }
            
            // Verify the search history belongs to the user
            SearchHistory searchHistory = searchHistoryRepository.findById(id).orElse(null);
            if (searchHistory == null || !searchHistory.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(404).build();
            }
            
            List<SimilaritySource> sources = similaritySourceRepository
                .findBySearchHistoryOrderBySimilarityPercentageDesc(searchHistory);
            
            return ResponseEntity.ok(sources);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}