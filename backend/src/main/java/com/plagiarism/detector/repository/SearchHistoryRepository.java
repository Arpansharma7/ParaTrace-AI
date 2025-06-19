package com.plagiarism.detector.repository;

import com.plagiarism.detector.model.SearchHistory;
import com.plagiarism.detector.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    
    // Find all searches by user
    Page<SearchHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // Find recent searches by user (last 10)
    List<SearchHistory> findTop10ByUserOrderByCreatedAtDesc(User user);
    
    // Find searches by user and date range
    List<SearchHistory> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(
        User user, LocalDateTime startDate, LocalDateTime endDate);
    
    // Find searches with high similarity scores
    List<SearchHistory> findByUserAndSimilarityScoreGreaterThanOrderBySimilarityScoreDesc(
        User user, Double threshold);
    
    // Find failed searches
    List<SearchHistory> findByUserAndStatusOrderByCreatedAtDesc(User user, SearchHistory.SearchStatus status);
    
    // Count total searches by user
    long countByUser(User user);
    
    // Find average similarity score by user
    @Query("SELECT AVG(sh.similarityScore) FROM SearchHistory sh WHERE sh.user = :user AND sh.similarityScore IS NOT NULL")
    Double findAverageSimilarityScoreByUser(@Param("user") User user);
    
    // Find searches with similar queries (for optimization)
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.searchQuery LIKE %:query% AND sh.user = :user ORDER BY sh.createdAt DESC")
    List<SearchHistory> findBySimilarQuery(@Param("query") String query, @Param("user") User user);
    
    // Find most common search patterns
    @Query("SELECT sh.searchQuery, COUNT(sh) as count FROM SearchHistory sh WHERE sh.user = :user GROUP BY sh.searchQuery ORDER BY count DESC")
    List<Object[]> findMostCommonQueries(@Param("user") User user);
} 