package com.codestats.service;

import com.codestats.alias.AliasResolver;
import com.codestats.config.CodeStatsConfig;
import com.codestats.git.GitLogParser;
import com.codestats.model.ContributorIdentity;
import com.codestats.model.ContributorStats;
import com.codestats.model.GitCommit;
import com.codestats.stats.StatisticsAggregator;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main service layer that orchestrates git analysis.
 * Wires together all components to provide end-to-end functionality.
 */
public class CodeStatsService {
    
    private final GitLogParser gitLogParser;
    private final AliasResolver aliasResolver;
    private final StatisticsAggregator statisticsAggregator;
    private final GitCommandExecutor gitCommandExecutor;
    
    public CodeStatsService(GitLogParser gitLogParser,
                           AliasResolver aliasResolver,
                           StatisticsAggregator statisticsAggregator,
                           GitCommandExecutor gitCommandExecutor) {
        this.gitLogParser = gitLogParser;
        this.aliasResolver = aliasResolver;
        this.statisticsAggregator = statisticsAggregator;
        this.gitCommandExecutor = gitCommandExecutor;
    }
    
    /**
     * Analyze a git repository and generate contributor statistics.
     * 
     * @param request Analysis request with all parameters
     * @return Analysis results with contributor statistics
     */
    public CodeStatsResult analyzeRepository(CodeStatsRequest request) {
        try {
            // Step 1: Execute git log command to get commit data
            String gitLogOutput = gitCommandExecutor.executeGitLog(
                request.repositoryPath(), 
                request.since(), 
                request.until(),
                request.includeUsers(),
                request.excludeUsers()
            );
            
            // Step 2: Parse git log output into structured commits
            List<GitCommit> allCommits = gitLogParser.parseCommits(gitLogOutput);
            
            // Step 3: Apply date filtering if needed
            List<GitCommit> filteredCommits = filterCommitsByDate(allCommits, request);
            
            // Step 4: Resolve contributor identities using alias configuration
            Map<String, ContributorIdentity> identities = aliasResolver.resolveIdentities(
                filteredCommits, 
                request.config().aliases()
            );
            
            // Step 5: Aggregate statistics for each contributor
            List<ContributorStats> contributorStats = statisticsAggregator.aggregateStatistics(
                filteredCommits,
                identities,
                request.config().languages(),
                request.config().productionDirectories(),
                request.config().testDirectories()
            );
            
            // Step 6: Calculate summary statistics
            int totalCommits = filteredCommits.size();
            
            return new CodeStatsResult(
                contributorStats,
                totalCommits,
                request.repositoryPath(),
                filteredCommits.isEmpty() ? null : filteredCommits.get(0).commitDate(),
                filteredCommits.isEmpty() ? null : filteredCommits.get(filteredCommits.size() - 1).commitDate(),
                true,
                null
            );
            
        } catch (Exception e) {
            return new CodeStatsResult(
                List.of(),
                0,
                request.repositoryPath(),
                null,
                null,
                false,
                "Error analyzing repository: " + e.getMessage()
            );
        }
    }
    
    /**
     * Apply additional date filtering if days parameter is specified.
     */
    private List<GitCommit> filterCommitsByDate(List<GitCommit> commits, CodeStatsRequest request) {
        if (request.days() == null) {
            return commits;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(request.days());
        return gitLogParser.filterByDateRange(commits, cutoffDate, null);
    }
    
    /**
     * Request object for repository analysis.
     */
    public record CodeStatsRequest(
        File repositoryPath,
        CodeStatsConfig config,
        Integer days,
        LocalDateTime since,
        LocalDateTime until,
        Set<String> includeUsers,
        Set<String> excludeUsers
    ) {
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private File repositoryPath = new File(".");
            private CodeStatsConfig config = CodeStatsConfig.getDefault();
            private Integer days;
            private LocalDateTime since;
            private LocalDateTime until;
            private Set<String> includeUsers = Set.of();
            private Set<String> excludeUsers = Set.of();
            
            public Builder repositoryPath(File repositoryPath) {
                this.repositoryPath = repositoryPath;
                return this;
            }
            
            public Builder config(CodeStatsConfig config) {
                this.config = config;
                return this;
            }
            
            public Builder days(Integer days) {
                this.days = days;
                return this;
            }
            
            public Builder since(LocalDateTime since) {
                this.since = since;
                return this;
            }
            
            public Builder until(LocalDateTime until) {
                this.until = until;
                return this;
            }
            
            public Builder includeUsers(Set<String> includeUsers) {
                this.includeUsers = Set.copyOf(includeUsers);
                return this;
            }
            
            public Builder excludeUsers(Set<String> excludeUsers) {
                this.excludeUsers = Set.copyOf(excludeUsers);
                return this;
            }
            
            public CodeStatsRequest build() {
                return new CodeStatsRequest(
                    repositoryPath, config, days, since, until, includeUsers, excludeUsers
                );
            }
        }
    }
    
    /**
     * Result object for repository analysis.
     */
    public record CodeStatsResult(
        List<ContributorStats> contributorStats,
        int totalCommits,
        File repositoryPath,
        LocalDateTime oldestCommit,
        LocalDateTime newestCommit,
        boolean success,
        String errorMessage
    ) {
        
        /**
         * Check if analysis was successful.
         */
        public boolean isSuccess() {
            return success;
        }
        
        /**
         * Get analysis period description.
         */
        public String getAnalysisPeriod() {
            if (oldestCommit == null || newestCommit == null) {
                return "No commits found";
            }
            
            if (oldestCommit.equals(newestCommit)) {
                return "Single commit on " + oldestCommit.toLocalDate();
            }
            
            return oldestCommit.toLocalDate() + " to " + newestCommit.toLocalDate();
        }
    }
}