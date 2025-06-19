package com.plagiarism.detector.repository;

import com.plagiarism.detector.model.SearchHistory;
import com.plagiarism.detector.model.SimilaritySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimilaritySourceRepository extends JpaRepository<SimilaritySource, Long> {
    
    
    List<SimilaritySource> findBySearchHistoryOrderBySimilarityPercentageDesc(SearchHistory searchHistory);
    
    
    List<SimilaritySource> findBySearchHistoryAndSimilarityPercentageGreaterThanOrderBySimilarityPercentageDesc(
        SearchHistory searchHistory, Double threshold);
    
    
    List<SimilaritySource> findByDomainOrderBySimilarityPercentageDesc(String domain);
    
    @Query("SELECT ss.domain, COUNT(ss) as count FROM SimilaritySource ss WHERE ss.domain IS NOT NULL GROUP BY ss.domain ORDER BY count DESC")
    List<Object[]> findMostCommonDomains();
    
    

@Query("SELECT ss FROM SimilaritySource ss WHERE ss.url LIKE %:urlPattern% ORDER BY ss.similarityPercentage DESC")
 List<SimilaritySource> findByUrlPattern(@Param("urlPattern") String urlPattern);
    
    
    @Query("SELECT ss.url, COUNT(ss) as count FROM SimilaritySource ss GROUP BY ss.url HAVING COUNT(ss) > 1 ORDER BY count DESC")
    List<Object[]> findDuplicateSources();
} 