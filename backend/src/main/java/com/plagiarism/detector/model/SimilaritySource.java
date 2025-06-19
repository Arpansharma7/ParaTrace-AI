package com.plagiarism.detector.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "similarity_sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimilaritySource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_history_id", nullable = false)
    private SearchHistory searchHistory;
    
    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;
    
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;
    
    @Column(name = "similarity_percentage", nullable = false)
    private Double similarityPercentage;
    
    @Column(name = "matched_text", columnDefinition = "LONGTEXT")
    private String matchedText;
    
    @Column(name = "domain")
    private String domain;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
} 