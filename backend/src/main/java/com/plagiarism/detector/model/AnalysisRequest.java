package com.plagiarism.detector.model;

public class AnalysisRequest {
    private String text;
    private boolean checkParaphrasing;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCheckParaphrasing() {
        return checkParaphrasing;
    }

    public void setCheckParaphrasing(boolean checkParaphrasing) {
        this.checkParaphrasing = checkParaphrasing;
    }
} 