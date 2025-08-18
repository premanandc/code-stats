package com.codestats.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Immutable record representing a single git commit with all relevant statistics. */
public record GitCommit(
    String hash,
    String authorName,
    String authorEmail,
    LocalDateTime commitDate,
    String message,
    List<FileChange> fileChanges,
    int insertions,
    int deletions) {

  /** Get total lines changed (insertions + deletions) */
  public int totalLinesChanged() {
    return insertions + deletions;
  }

  /** Get net lines (insertions - deletions) */
  public int netLines() {
    return insertions - deletions;
  }

  /** Get files changed count */
  public int filesChangedCount() {
    return fileChanges.size();
  }

  /** Group file changes by language based on file extensions and filenames */
  public Map<String, List<FileChange>> getChangesByLanguage(
      Map<String, String> extensionToLanguage, Map<String, String> filenameToLanguage) {
    return fileChanges.stream()
        .collect(
            java.util.stream.Collectors.groupingBy(
                fileChange ->
                    getLanguageForFile(
                        fileChange.path(), extensionToLanguage, filenameToLanguage)));
  }

  private String getLanguageForFile(
      String filePath,
      Map<String, String> extensionToLanguage,
      Map<String, String> filenameToLanguage) {
    int lastDot = filePath.lastIndexOf('.');
    String fileName = filePath.substring(filePath.lastIndexOf('/') + 1).toLowerCase();

    // First check if the complete filename (without path) matches our filename mappings
    if (filenameToLanguage.containsKey(fileName)) {
      return filenameToLanguage.get(fileName);
    }

    // Handle files without extensions
    if (lastDot == -1) {
      return "Unknown";
    }

    // Check for extension mapping, but only if the extension doesn't contain slashes
    String extension = filePath.substring(lastDot + 1).toLowerCase();
    if (!extension.contains("/")) {
      return extensionToLanguage.getOrDefault(extension, "Unknown");
    }

    return "Unknown";
  }
}
