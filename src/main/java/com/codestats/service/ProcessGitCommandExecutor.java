package com.codestats.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Real implementation of GitCommandExecutor that executes actual git commands. */
public class ProcessGitCommandExecutor implements GitCommandExecutor {

  @Override
  public String executeGitLog(
      File repositoryPath,
      LocalDateTime since,
      LocalDateTime until,
      Integer maxCommits,
      Set<String> includeUsers,
      Set<String> excludeUsers) {

    if (!isGitRepository(repositoryPath)) {
      throw new RuntimeException("Not a git repository: " + repositoryPath.getAbsolutePath());
    }

    try {
      List<String> command = buildGitLogCommandList(since, until, maxCommits, includeUsers, excludeUsers);

      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.directory(repositoryPath);
      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      StringBuilder output = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }

      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new RuntimeException(
            "Git command failed with exit code " + exitCode + ": " + output.toString());
      }

      return output.toString();

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Failed to execute git command", e);
    }
  }

  /** Build git log command as list of arguments for ProcessBuilder. */
  private List<String> buildGitLogCommandList(
      LocalDateTime since,
      LocalDateTime until,
      Integer maxCommits,
      Set<String> includeUsers,
      Set<String> excludeUsers) {
    List<String> command = new ArrayList<>();
    command.add("git");
    command.add("log");
    command.add("--numstat");
    command.add("--pretty=format:commit %H%nAuthor: %an <%ae>%nDate:   %ad%n%n    %s%n");
    command.add("--date=iso");
    command.add("--no-merges"); // Skip merge commits for cleaner stats

    // Date filters with proper sanitization
    if (since != null) {
      command.add("--since=" + sanitizeGitParameter(formatDateForGit(since)));
    }
    if (until != null) {
      command.add("--until=" + sanitizeGitParameter(formatDateForGit(until)));
    }

    // Limit number of commits
    if (maxCommits != null) {
      command.add("--max-count=" + maxCommits);
    }

    // User filters with sanitization
    if (includeUsers != null && !includeUsers.isEmpty()) {
      for (String user : includeUsers) {
        String sanitizedUser = sanitizeGitParameter(user);
        if (sanitizedUser != null) {
          command.add("--author=" + sanitizedUser);
        }
      }
    }

    // Note: excludeUsers would be handled in post-processing
    // Git doesn't have a direct --exclude-author option

    return command;
  }

  /**
   * Sanitize git command parameters to prevent command injection. Only allows safe characters for
   * git parameters.
   */
  private String sanitizeGitParameter(String parameter) {
    if (parameter == null || parameter.trim().isEmpty()) {
      return null;
    }

    // Remove leading/trailing whitespace
    String sanitized = parameter.trim();

    // Only allow alphanumeric characters, dots, dashes, underscores, @, spaces, and colons (for
    // dates/emails)
    if (!sanitized.matches("[\\w\\s@.:-]+")) {
      throw new IllegalArgumentException("Invalid characters in git parameter: " + parameter);
    }

    // Prevent command injection patterns
    if (sanitized.contains("&&")
        || sanitized.contains("||")
        || sanitized.contains(";")
        || sanitized.contains("|")
        || sanitized.contains("$(")
        || sanitized.contains("`")
        || sanitized.contains("$")) {
      throw new IllegalArgumentException("Potentially dangerous git parameter: " + parameter);
    }

    return sanitized;
  }
}
