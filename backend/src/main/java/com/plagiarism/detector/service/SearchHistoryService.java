package com.plagiarism.detector.service;

import com.plagiarism.detector.model.SearchHistory;
import com.plagiarism.detector.model.SimilaritySource;
import com.plagiarism.detector.model.User;
import com.plagiarism.detector.repository.SearchHistoryRepository;
import com.plagiarism.detector.repository.SimilaritySourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchHistoryService {
    
    private final SearchHistoryRepository searchHistoryRepository;
    private final SimilaritySourceRepository similaritySourceRepository;
    
    @Autowired
    public SearchHistoryService(
            SearchHistoryRepository searchHistoryRepository,
            SimilaritySourceRepository similaritySourceRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.similaritySourceRepository = similaritySourceRepository;
    }
    
    public Page<SearchHistory> getUserSearchHistory(User user, Pageable pageable) {
        return searchHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    public List<SearchHistory> getRecentSearches(User user, int limit) {
        return searchHistoryRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }
    
    public List<SearchHistory> getSearchesByDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return searchHistoryRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, startDate, endDate);
    }
    
    public List<SearchHistory> getHighSimilaritySearches(User user, double threshold) {
        return searchHistoryRepository.findByUserAndSimilarityScoreGreaterThanOrderBySimilarityScoreDesc(user, threshold);
    }
    
    public List<SearchHistory> getFailedSearches(User user) {
        return searchHistoryRepository.findByUserAndStatusOrderByCreatedAtDesc(user, SearchHistory.SearchStatus.FAILED);
    }
    
    public List<SimilaritySource> getSearchSources(SearchHistory searchHistory) {
        return similaritySourceRepository.findBySearchHistoryOrderBySimilarityPercentageDesc(searchHistory);
    }
    
    public Map<String, Object> getUserStatistics(User user) {
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
        
        // Success rate
        double successRate = totalSearches > 0 ? 
            ((double) (totalSearches - failedSearches.size()) / totalSearches) * 100 : 0.0;
        statistics.put("successRate", Math.round(successRate * 100.0) / 100.0);
        
        // Most common search patterns
        List<Object[]> commonQueries = searchHistoryRepository.findMostCommonQueries(user);
        statistics.put("commonQueries", commonQueries);
        
        return statistics;
    }
    
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // Most common domains
        List<Object[]> commonDomains = similaritySourceRepository.findMostCommonDomains();
        statistics.put("commonDomains", commonDomains);
        
        // Duplicate sources
        List<Object[]> duplicateSources = similaritySourceRepository.findDuplicateSources();
        statistics.put("duplicateSources", duplicateSources);
        
        return statistics;
    }
    
    public void cleanupOldSearches(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        // This would require a custom query to implement cleanup
        // For now, we'll just log the intention
        System.out.println("Cleanup requested for searches older than " + daysToKeep + " days");
    }
} 