package com.plagiarism.detector.model;

import java.util.List;

public class AnalysisResponse {
    private double similarityScore;
    private List<SimilaritySource> matchedSources;
    private String paraphrasedText;
    private boolean aiDetected;
    private double aiConfidence;
    private String error;

    public static class SimilaritySource {
        private String url;
        private String title;
        private double similarityPercentage;
        private String matchedText;

        public SimilaritySource() {}

        public SimilaritySource(String url, String title, double similarityPercentage, String matchedText) {
            this.url = url;
            this.title = title;
            this.similarityPercentage = similarityPercentage;
            this.matchedText = matchedText;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public double getSimilarityPercentage() {
            return similarityPercentage;
        }

        public void setSimilarityPercentage(double similarityPercentage) {
            this.similarityPercentage = similarityPercentage;
        }

        public String getMatchedText() {
            return matchedText;
        }

        public void setMatchedText(String matchedText) {
            this.matchedText = matchedText;
        }
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public List<SimilaritySource> getMatchedSources() {
        return matchedSources;
    }

    public void setMatchedSources(List<SimilaritySource> matchedSources) {
        this.matchedSources = matchedSources;
    }

    public String getParaphrasedText() {
        return paraphrasedText;
    }

    public void setParaphrasedText(String paraphrasedText) {
        this.paraphrasedText = paraphrasedText;
    }

    public boolean isAiDetected() {
        return aiDetected;
    }

    public void setAiDetected(boolean aiDetected) {
        this.aiDetected = aiDetected;
    }

    public double getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
} 