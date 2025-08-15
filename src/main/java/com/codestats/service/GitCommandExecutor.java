package com.codestats.service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Interface for executing git commands to retrieve commit history.
 * Abstracted to allow testing with mock implementations.
 */
public interface GitCommandExecutor {
    
    /**
     * Execute git log command with specified filters.
     * 
     * @param repositoryPath Path to git repository
     * @param since Start date filter (optional)
     * @param until End date filter (optional)
     * @param includeUsers Set of user emails to include (optional)
     * @param excludeUsers Set of user emails to exclude (optional)
     * @return Git log output as string
     * @throws RuntimeException if git command fails
     */
    String executeGitLog(File repositoryPath,
                        LocalDateTime since,
                        LocalDateTime until,
                        Set<String> includeUsers,
                        Set<String> excludeUsers);
    
    /**
     * Check if a directory is a valid git repository.
     * 
     * @param repositoryPath Path to check
     * @return true if it's a git repository
     */
    default boolean isGitRepository(File repositoryPath) {
        File gitDir = new File(repositoryPath, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }
    
    /**
     * Format LocalDateTime for git command.
     */
    default String formatDateForGit(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    /**
     * Build git log command arguments.
     */
    default String buildGitLogCommand(LocalDateTime since,
                                    LocalDateTime until,
                                    Set<String> includeUsers,
                                    Set<String> excludeUsers) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("git log --stat --pretty=fuller");
        
        // Date filters
        if (since != null) {
            cmd.append(" --since=\"").append(formatDateForGit(since)).append("\"");
        }
        if (until != null) {
            cmd.append(" --until=\"").append(formatDateForGit(until)).append("\"");
        }
        
        // User filters
        if (includeUsers != null && !includeUsers.isEmpty()) {
            for (String user : includeUsers) {
                cmd.append(" --author=\"").append(user).append("\"");
            }
        }
        
        // Note: git doesn't have direct exclude-author, would need post-processing
        // For now, we'll handle excludeUsers in the parsing stage
        
        return cmd.toString();
    }
}