// Analysis page JavaScript
class AnalysisPage {
    constructor() {
        this.apiBaseUrl = 'http://localhost:8080/api';
        this.token = localStorage.getItem('auth_token');
        try {
            this.currentUser = JSON.parse(localStorage.getItem('user_data'));
        } catch (e) {
            this.currentUser = null;
        }
        
        this.initializeElements();
        this.setupEventListeners();
        this.checkAuthentication();
        this.loadStatistics();
    }
    
    initializeElements() {
        this.textInput = document.getElementById('textInput');
        this.analyzeBtn = document.getElementById('analyzeBtn');
        this.loading = document.getElementById('loading');
        this.resultsContent = document.getElementById('resultsContent');
        this.statsContent = document.getElementById('statsContent');
        this.historyContent = document.getElementById('historyContent');
        this.logoutBtn = document.getElementById('logoutBtn');
        
        // Tab elements
        this.historyTabs = document.querySelectorAll('.history-tab');
        this.tabContents = document.querySelectorAll('.tab-content');
    }
    
    setupEventListeners() {
        this.analyzeBtn.addEventListener('click', () => this.analyzeText());
        this.logoutBtn.addEventListener('click', () => this.logout());
        
        // Tab switching
        this.historyTabs.forEach(tab => {
            tab.addEventListener('click', () => this.switchTab(tab.dataset.tab));
        });
        
        // Text input validation
        this.textInput.addEventListener('input', () => {
            this.validateInput();
        });
    }
    
    checkAuthentication() {
        if (!this.token || !this.currentUser) {
            window.location.href = 'auth.html';
            return;
        }
        
        // Verify token is still valid
        this.verifyToken();
    }
    
    async verifyToken() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/health`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            if (!response.ok) {
                throw new Error('Token invalid');
            }
        } catch (error) {
            console.error('Authentication error:', error);
            this.logout();
        }
    }
    
    validateInput() {
        const text = this.textInput.value.trim();
        const isValid = text.length >= 10;
        
        this.analyzeBtn.disabled = !isValid;
        
        if (isValid) {
            this.analyzeBtn.style.opacity = '1';
        } else {
            this.analyzeBtn.style.opacity = '0.6';
        }
    }
    
    async analyzeText() {
        const text = this.textInput.value.trim();
        const checkParaphrasing = document.getElementById('checkParaphrasing').checked;
        
        if (text.length < 10) {
            this.showError('Please enter at least 10 characters of text to analyze.');
            return;
        }
        
        this.showLoading(true);
        
        try {
            const requestBody = {
                text: text,
                checkParaphrasing: checkParaphrasing
            };
            
            const response = await fetch(`${this.apiBaseUrl}/analyze`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                },
                body: JSON.stringify(requestBody)
            });
            
            const result = await response.json();
            
            if (!response.ok) {
                throw new Error(result.error || 'Analysis failed');
            }
            
            this.displayResults(result);
            this.loadStatistics(); // Refresh statistics
            
        } catch (error) {
            console.error('Analysis error:', error);
            this.showError(`Analysis failed: ${error.message}`);
        } finally {
            this.showLoading(false);
        }
    }
    
    displayResults(result) {
        if (result.error) {
            this.resultsContent.innerHTML = `
                <div style="text-align: center; color: #ff6b6b;">
                    <i class="fas fa-exclamation-triangle" style="font-size: 48px; margin-bottom: 15px;"></i>
                    <h3>Analysis Error</h3>
                    <p>${result.error}</p>
                </div>
            `;
            return;
        }
        
        const similarityScore = (result.similarityScore * 100).toFixed(1);
        const aiDetected = result.aiDetected;
        const aiConfidence = (result.aiConfidence * 100).toFixed(1);
        
        let resultsHtml = `
            <div class="similarity-score">
                <div class="score-circle" style="background: ${this.getScoreColor(similarityScore)};">
                    ${similarityScore}%
                </div>
                <div class="score-label">Similarity Score</div>
            </div>
            
            <div class="ai-detection">
                <div class="ai-status">
                    <i class="fas fa-robot ai-icon" style="color: ${aiDetected ? '#ff6b6b' : '#4CAF50'};"></i>
                    <span><strong>AI Detection:</strong> ${aiDetected ? 'Detected' : 'Not Detected'}</span>
                </div>
                <div>Confidence: ${aiConfidence}%</div>
            </div>
        `;
        
        if (result.paraphrasedText) {
            resultsHtml += `
                <div style="background: rgba(255, 255, 255, 0.1); border-radius: 15px; padding: 20px; margin-bottom: 20px;">
                    <h4><i class="fas fa-exchange-alt"></i> Paraphrased Version</h4>
                    <p style="font-style: italic; opacity: 0.9;">${result.paraphrasedText}</p>
                </div>
            `;
        }
        
        if (result.matchedSources && result.matchedSources.length > 0) {
            resultsHtml += `
                <h4><i class="fas fa-link"></i> Matched Sources (${result.matchedSources.length})</h4>
                <div class="sources-list">
            `;
            
            result.matchedSources.forEach(source => {
                resultsHtml += `
                    <div class="source-item">
                        <div class="source-title">${source.title || 'Untitled'}</div>
                        <div class="source-url">${source.url}</div>
                        <div class="source-similarity">Similarity: ${source.similarityPercentage.toFixed(1)}%</div>
                        <div class="source-text">${source.matchedText}</div>
                    </div>
                `;
            });
            
            resultsHtml += '</div>';
        } else {
            resultsHtml += `
                <div style="text-align: center; padding: 20px; opacity: 0.7;">
                    <i class="fas fa-check-circle" style="font-size: 48px; color: #4CAF50; margin-bottom: 15px;"></i>
                    <h4>No Similar Content Found</h4>
                    <p>Your text appears to be original!</p>
                </div>
            `;
        }
        
        this.resultsContent.innerHTML = resultsHtml;
    }
    
    getScoreColor(score) {
        if (score < 30) return '#4CAF50'; // Green
        if (score < 70) return '#FFC107'; // Yellow
        return '#FF5722'; // Red
    }
    
    async loadStatistics() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/statistics`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            if (!response.ok) {
                throw new Error('Failed to load statistics');
            }
            
            const stats = await response.json();
            this.displayStatistics(stats);
            
        } catch (error) {
            console.error('Statistics error:', error);
            this.statsContent.innerHTML = `
                <p style="text-align: center; color: #ff6b6b;">
                    Failed to load statistics: ${error.message}
                </p>
            `;
        }
    }
    
    displayStatistics(stats) {
        const statsHtml = `
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-value">${stats.totalSearches || 0}</div>
                    <div class="stat-label">Total Searches</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${(stats.averageSimilarity || 0).toFixed(1)}%</div>
                    <div class="stat-label">Avg Similarity</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${stats.recentSearches || 0}</div>
                    <div class="stat-label">Recent (7 days)</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${stats.highSimilaritySearches || 0}</div>
                    <div class="stat-label">High Similarity</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${stats.failedSearches || 0}</div>
                    <div class="stat-label">Failed Searches</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${((stats.totalSearches - (stats.failedSearches || 0)) / Math.max(stats.totalSearches, 1) * 100).toFixed(1)}%</div>
                    <div class="stat-label">Success Rate</div>
                </div>
            </div>
        `;
        
        this.statsContent.innerHTML = statsHtml;
    }
    
    async loadHistory() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/history?page=0&size=10`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            if (!response.ok) {
                throw new Error('Failed to load history');
            }
            
            const historyData = await response.json();
            this.displayHistory(historyData.content || []);
            
        } catch (error) {
            console.error('History error:', error);
            this.historyContent.innerHTML = `
                <p style="text-align: center; color: #ff6b6b;">
                    Failed to load history: ${error.message}
                </p>
            `;
        }
    }
    
    displayHistory(history) {
        if (history.length === 0) {
            this.historyContent.innerHTML = `
                <p style="text-align: center; opacity: 0.7;">
                    No search history found. Start analyzing text to see your history here.
                </p>
            `;
            return;
        }
        
        let historyHtml = '<div class="history-list">';
        
        history.forEach(item => {
            const date = new Date(item.createdAt).toLocaleDateString();
            const time = new Date(item.createdAt).toLocaleTimeString();
            const similarity = item.similarityScore ? (item.similarityScore * 100).toFixed(1) + '%' : 'N/A';
            const status = item.status;
            
            historyHtml += `
                <div class="history-item" onclick="analysisPage.viewHistoryItem(${item.id})">
                    <div class="history-text">${item.searchQuery.substring(0, 100)}${item.searchQuery.length > 100 ? '...' : ''}</div>
                    <div class="history-meta">
                        <span>Similarity: ${similarity}</span>
                        <span>Status: ${status}</span>
                        <span>${date} ${time}</span>
                    </div>
                </div>
            `;
        });
        
        historyHtml += '</div>';
        this.historyContent.innerHTML = historyHtml;
    }
    
    async viewHistoryItem(id) {
        try {
            const response = await fetch(`${this.apiBaseUrl}/history/${id}/sources`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            
            if (!response.ok) {
                throw new Error('Failed to load sources');
            }
            
            const sources = await response.json();
            
            // Display sources in a modal or update the results section
            this.displayHistorySources(sources);
            
        } catch (error) {
            console.error('History item error:', error);
            this.showError(`Failed to load sources: ${error.message}`);
        }
    }
    
    displayHistorySources(sources) {
        let sourcesHtml = `
            <h4><i class="fas fa-history"></i> Historical Sources</h4>
            <div class="sources-list">
        `;
        
        if (sources.length === 0) {
            sourcesHtml += `
                <p style="text-align: center; opacity: 0.7;">No sources found for this search.</p>
            `;
        } else {
            sources.forEach(source => {
                sourcesHtml += `
                    <div class="source-item">
                        <div class="source-title">${source.title || 'Untitled'}</div>
                        <div class="source-url">${source.url}</div>
                        <div class="source-similarity">Similarity: ${source.similarityPercentage.toFixed(1)}%</div>
                        <div class="source-text">${source.matchedText}</div>
                    </div>
                `;
            });
        }
        
        sourcesHtml += '</div>';
        
        // Update results section with historical data
        this.resultsContent.innerHTML = sourcesHtml;
    }
    
    switchTab(tabName) {
        // Update tab buttons
        this.historyTabs.forEach(tab => {
            tab.classList.toggle('active', tab.dataset.tab === tabName);
        });
        
        // Update tab content
        this.tabContents.forEach(content => {
            content.style.display = content.id === `${tabName}Tab` ? 'block' : 'none';
        });
        
        // Load content based on tab
        if (tabName === 'history') {
            this.loadHistory();
        } else if (tabName === 'statistics') {
            this.loadStatistics();
        }
    }
    
    showLoading(show) {
        this.loading.style.display = show ? 'block' : 'none';
        this.analyzeBtn.disabled = show;
    }
    
    showError(message) {
        this.resultsContent.innerHTML = `
            <div style="text-align: center; color: #ff6b6b;">
                <i class="fas fa-exclamation-triangle" style="font-size: 48px; margin-bottom: 15px;"></i>
                <h3>Error</h3>
                <p>${message}</p>
            </div>
        `;
    }
    
    logout() {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user_data');
        window.location.href = 'auth.html';
    }
}

// Initialize the analysis page
const analysisPage = new AnalysisPage(); 