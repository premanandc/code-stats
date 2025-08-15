package com.codestats.git;

import com.codestats.model.GitCommit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Functional interface for parsing git log output into structured commit data.
 * Pure functional approach - no side effects, immutable data.
 */
public interface GitLogParser {
    
    /**
     * Parse git log output into a list of GitCommit objects.
     * 
     * @param gitLogOutput Raw git log output from command line
     * @return List of parsed commits, empty if no commits found
     */
    List<GitCommit> parseCommits(String gitLogOutput);
    
    /**
     * Parse a single commit from git log output.
     * 
     * @param commitBlock Single commit block from git log
     * @return Optional GitCommit if parsing successful, empty otherwise
     */
    Optional<GitCommit> parseCommit(String commitBlock);
    
    /**
     * Filter commits by date range.
     * 
     * @param commits List of commits to filter
     * @param since Start date (inclusive), null for no start limit
     * @param until End date (inclusive), null for no end limit
     * @return Filtered list of commits
     */
    default List<GitCommit> filterByDateRange(List<GitCommit> commits, 
                                             LocalDateTime since, 
                                             LocalDateTime until) {
        return commits.stream()
            .filter(commit -> {
                LocalDateTime commitDate = commit.commitDate();
                boolean afterSince = since == null || !commitDate.isBefore(since);
                boolean beforeUntil = until == null || !commitDate.isAfter(until);
                return afterSince && beforeUntil;
            })
            .toList();
    }
    
    /**
     * Filter commits by author emails.
     * 
     * @param commits List of commits to filter
     * @param emails Set of author emails to include
     * @return Filtered list of commits
     */
    default List<GitCommit> filterByAuthors(List<GitCommit> commits, 
                                           java.util.Set<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return commits;
        }
        
        return commits.stream()
            .filter(commit -> emails.contains(commit.authorEmail()))
            .toList();
    }
}