package com.codestats.model;

/** Immutable record representing statistics for a specific programming language. */
public record LanguageStats(
    String language, int linesChanged, int insertions, int deletions, int filesChanged) {

  /** Calculate net lines (insertions - deletions) */
  public int netLines() {
    return insertions - deletions;
  }

  /** Combine two LanguageStats into one */
  public LanguageStats combine(LanguageStats other) {
    if (!this.language.equals(other.language)) {
      throw new IllegalArgumentException("Cannot combine stats for different languages");
    }

    return new LanguageStats(
        language,
        this.linesChanged + other.linesChanged,
        this.insertions + other.insertions,
        this.deletions + other.deletions,
        this.filesChanged + other.filesChanged);
  }
}
