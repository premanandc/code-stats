package com.codestats.model;

/**
 * Immutable record representing a file change in a git commit.
 */
public record FileChange(
    String path,
    int insertions,
    int deletions,
    ChangeType changeType
) {
    
    /**
     * Get total lines changed for this file
     */
    public int totalLinesChanged() {
        return insertions + deletions;
    }
    
    /**
     * Get net lines changed for this file
     */
    public int netLines() {
        return insertions - deletions;
    }
    
    /**
     * Type of change made to the file
     */
    public enum ChangeType {
        ADDED,      // New file
        MODIFIED,   // Existing file changed
        DELETED,    // File removed
        RENAMED,    // File moved/renamed
        COPIED      // File copied
    }
}