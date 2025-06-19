package com.plagiarism.detector.service;

import com.plagiarism.detector.model.AnalysisRequest;
import com.plagiarism.detector.model.AnalysisResponse;
import com.plagiarism.detector.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIService {
    
    private final PlagiarismAnalysisService plagiarismAnalysisService;
    
    @Autowired
    public AIService(PlagiarismAnalysisService plagiarismAnalysisService) {
        this.plagiarismAnalysisService = plagiarismAnalysisService;
    }
    
    public AnalysisResponse analyzeText(AnalysisRequest request, User user) {
        return plagiarismAnalysisService.analyzeText(request, user);
    }
    
    public AnalysisResponse analyzeText(AnalysisRequest request) {
        // For backward compatibility, create a default user
        User defaultUser = new User();
        defaultUser.setId(1L);
        defaultUser.setUsername("default");
        defaultUser.setEmail("default@example.com");
        defaultUser.setPassword("default");
        
        return plagiarismAnalysisService.analyzeText(request, defaultUser);
    }
}