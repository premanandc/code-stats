package com.codestats.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Immutable record representing a single git commit with all relevant statistics.
 */
public record GitCommit(
    String hash,
    String authorName,
    String authorEmail,
    LocalDateTime commitDate,
    String message,
    List<FileChange> fileChanges,
    int insertions,
    int deletions
) {
    
    /**
     * Get total lines changed (insertions + deletions)
     */
    public int totalLinesChanged() {
        return insertions + deletions;
    }
    
    /**
     * Get net lines (insertions - deletions)
     */
    public int netLines() {
        return insertions - deletions;
    }
    
    /**
     * Get files changed count
     */
    public int filesChangedCount() {
        return fileChanges.size();
    }
    
    /**
     * Group file changes by language based on file extensions
     */
    public Map<String, List<FileChange>> getChangesByLanguage(Map<String, String> extensionToLanguage) {
        return fileChanges.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                fileChange -> getLanguageForFile(fileChange.path(), extensionToLanguage)
            ));
    }
    
    private String getLanguageForFile(String filePath, Map<String, String> extensionToLanguage) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot == -1) return "Unknown";
        
        String extension = filePath.substring(lastDot + 1).toLowerCase();
        return extensionToLanguage.getOrDefault(extension, "Unknown");
    }
}