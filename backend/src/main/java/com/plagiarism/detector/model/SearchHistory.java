package com.plagiarism.detector.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "search_query", nullable = false, columnDefinition = "TEXT")
    private String searchQuery;
    
    @Column(name = "text_length")
    private Integer textLength;
    
    @Column(name = "similarity_score")
    private Double similarityScore;
    
    @Column(name = "ai_detected")
    private Boolean aiDetected;
    
    @Column(name = "ai_confidence")
    private Double aiConfidence;
    
    @Column(name = "sources_found")
    private Integer sourcesFound;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SearchStatus status = SearchStatus.COMPLETED;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SearchStatus.COMPLETED;
        }
    }
    
    public enum SearchStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
} 