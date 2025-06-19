package com.plagiarism.detector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plagiarism.detector.model.*;
import com.plagiarism.detector.repository.SearchHistoryRepository;
import com.plagiarism.detector.repository.SimilaritySourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PlagiarismAnalysisService {
    
    @Value("${ai.service.python.path:ai_service/ai_similarity.py}")
    private String pythonScriptPath;
    
    @Value("${ai.service.python.command:python}")
    private String pythonCommand;
    
    private final SearchHistoryRepository searchHistoryRepository;
    private final SimilaritySourceRepository similaritySourceRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public PlagiarismAnalysisService(
            SearchHistoryRepository searchHistoryRepository,
            SimilaritySourceRepository similaritySourceRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.similaritySourceRepository = similaritySourceRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    public AnalysisResponse analyzeText(AnalysisRequest request, User user) {
        long startTime = System.currentTimeMillis();
        
        // Create search history record
        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setUser(user);
        searchHistory.setSearchQuery(request.getText());
        searchHistory.setTextLength(request.getText().length());
        searchHistory.setStatus(SearchHistory.SearchStatus.PROCESSING);
        searchHistory = searchHistoryRepository.save(searchHistory);
        
        try {
            // Call Python AI service
            Map<String, Object> pythonResult = callPythonAIService(request.getText(), request.isCheckParaphrasing());
            
            // Parse Python response
            AnalysisResponse response = parsePythonResponse(pythonResult);
            
            // Save sources to database
            if (response.getMatchedSources() != null) {
                for (AnalysisResponse.SimilaritySource source : response.getMatchedSources()) {
                    SimilaritySource dbSource = new SimilaritySource();
                    dbSource.setSearchHistory(searchHistory);
                    dbSource.setUrl(source.getUrl());
                    dbSource.setTitle(source.getTitle());
                    dbSource.setSimilarityPercentage(source.getSimilarityPercentage());
                    dbSource.setMatchedText(source.getMatchedText());
                    dbSource.setDomain(extractDomain(source.getUrl()));
                    similaritySourceRepository.save(dbSource);
                }
            }
            
            // Update search history
            long processingTime = System.currentTimeMillis() - startTime;
            searchHistory.setSimilarityScore(response.getSimilarityScore());
            searchHistory.setAiDetected(response.isAiDetected());
            searchHistory.setAiConfidence(response.getAiConfidence());
            searchHistory.setSourcesFound(response.getMatchedSources() != null ? response.getMatchedSources().size() : 0);
            searchHistory.setProcessingTimeMs(processingTime);
            searchHistory.setStatus(SearchHistory.SearchStatus.COMPLETED);
            searchHistoryRepository.save(searchHistory);
            
            return response;
            
        } catch (Exception e) {
            // Update search history with error
            searchHistory.setStatus(SearchHistory.SearchStatus.FAILED);
            searchHistory.setErrorMessage(e.getMessage());
            searchHistoryRepository.save(searchHistory);
            
            AnalysisResponse errorResponse = new AnalysisResponse();
            errorResponse.setError("Analysis failed: " + e.getMessage());
            return errorResponse;
        }
    }
    
    private Map<String, Object> callPythonAIService(String text, boolean checkParaphrasing) throws IOException, InterruptedException {
        // Build command to call Python script
        List<String> command = new ArrayList<>();
        command.add(pythonCommand);
        command.add(pythonScriptPath);
        command.add("--text");
        command.add(text);
        command.add("--format");
        command.add("json");
        
        if (checkParaphrasing) {
            command.add("--paraphrasing");
        }
        
        // Execute Python script
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // Wait for process to complete
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("Python script failed with exit code: " + exitCode + "\nOutput: " + output.toString());
        }
        
        // Parse JSON response
        String jsonOutput = output.toString().trim();
        return objectMapper.readValue(jsonOutput, Map.class);
    }
    
    private AnalysisResponse parsePythonResponse(Map<String, Object> pythonResult) {
        AnalysisResponse response = new AnalysisResponse();
        
        // Check for error
        if (pythonResult.containsKey("error") && pythonResult.get("error") != null) {
            response.setError((String) pythonResult.get("error"));
            return response;
        }
        
        // Parse similarity score
        if (pythonResult.containsKey("similarityScore")) {
            response.setSimilarityScore(((Number) pythonResult.get("similarityScore")).doubleValue());
        }
        
        // Parse AI detection
        if (pythonResult.containsKey("aiDetected")) {
            response.setAiDetected((Boolean) pythonResult.get("aiDetected"));
        }
        
        if (pythonResult.containsKey("aiConfidence")) {
            response.setAiConfidence(((Number) pythonResult.get("aiConfidence")).doubleValue());
        }
        
        // Parse paraphrased text
        if (pythonResult.containsKey("paraphrasedText")) {
            response.setParaphrasedText((String) pythonResult.get("paraphrasedText"));
        }
        
        // Parse matched sources
        if (pythonResult.containsKey("matchedSources")) {
            List<Map<String, Object>> sourcesData = (List<Map<String, Object>>) pythonResult.get("matchedSources");
            List<AnalysisResponse.SimilaritySource> sources = new ArrayList<>();
            
            for (Map<String, Object> sourceData : sourcesData) {
                AnalysisResponse.SimilaritySource source = new AnalysisResponse.SimilaritySource(
                    (String) sourceData.get("url"),
                    (String) sourceData.get("title"),
                    ((Number) sourceData.get("similarityPercentage")).doubleValue(),
                    (String) sourceData.get("matchedText")
                );
                sources.add(source);
            }
            
            response.setMatchedSources(sources);
        }
        
        return response;
    }
    
    private String extractDomain(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }
} 