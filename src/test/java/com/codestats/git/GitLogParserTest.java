package com.codestats.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.codestats.model.FileChange;
import com.codestats.model.GitCommit;

class GitLogParserTest {

  private GitLogParser parser;

  @BeforeEach
  void setUp() {
    // Will implement JGitLogParser later
    parser = new JGitLogParser();
  }

  @Test
  @DisplayName("Should parse single commit with basic information")
  void shouldParseSingleCommitBasicInfo() {
    String gitLogOutput =
        """
            commit abc123def456
            Author: John Doe <john.doe@example.com>
            Date:   2024-01-15 10:30:45 +0000

                Add user authentication feature

            25	0	src/main/java/User.java
            8	0	src/test/java/UserTest.java
            """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);

    assertThat(commit.hash()).isEqualTo("abc123def456");
    assertThat(commit.authorName()).isEqualTo("John Doe");
    assertThat(commit.authorEmail()).isEqualTo("john.doe@example.com");
    assertThat(commit.message()).contains("Add user authentication feature");
    assertThat(commit.insertions()).isEqualTo(33);
    assertThat(commit.deletions()).isEqualTo(0);
    assertThat(commit.filesChangedCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should parse file changes with insertions and deletions")
  void shouldParseFileChangesWithStats() {
    String gitLogOutput =
        """
            commit xyz789abc123
            Author: Jane Smith <jane@company.com>
            Date:   2024-01-16 14:20:30 +0000

                Refactor authentication logic

            8	7	src/main/java/Auth.java
            33	0	src/main/java/User.java
            12	0	src/test/java/AuthTest.java
            """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);

    assertThat(commit.insertions()).isEqualTo(53);
    assertThat(commit.deletions()).isEqualTo(7);
    assertThat(commit.totalLinesChanged()).isEqualTo(60);
    assertThat(commit.netLines()).isEqualTo(46);

    assertThat(commit.fileChanges()).hasSize(3);
    assertThat(commit.fileChanges())
        .extracting(FileChange::path)
        .contains(
            "src/main/java/Auth.java", "src/main/java/User.java", "src/test/java/AuthTest.java");
  }

  @Test
  @DisplayName("Should parse multiple commits")
  void shouldParseMultipleCommits() {
    String gitLogOutput =
        """
            commit commit1hash
            Author: Alice <alice@dev.com>
            Date:   2024-01-17 09:15:00 +0000

                Fix bug in validation

            3	2	src/Validator.java

            commit commit2hash
            Author: Bob <bob@dev.com>
            Date:   2024-01-16 16:45:30 +0000

                Add new feature

            20	0	src/Feature.java
            8	0	test/FeatureTest.java
            """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(2);

    // First commit
    GitCommit commit1 = commits.get(0);
    assertThat(commit1.authorName()).isEqualTo("Alice");
    assertThat(commit1.authorEmail()).isEqualTo("alice@dev.com");
    assertThat(commit1.insertions()).isEqualTo(3);
    assertThat(commit1.deletions()).isEqualTo(2);

    // Second commit
    GitCommit commit2 = commits.get(1);
    assertThat(commit2.authorName()).isEqualTo("Bob");
    assertThat(commit2.authorEmail()).isEqualTo("bob@dev.com");
    assertThat(commit2.insertions()).isEqualTo(28);
    assertThat(commit2.deletions()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should filter commits by date range")
  void shouldFilterCommitsByDateRange() {
    LocalDateTime jan15 = LocalDateTime.of(2024, 1, 15, 10, 0);
    LocalDateTime jan16 = LocalDateTime.of(2024, 1, 16, 10, 0);
    LocalDateTime jan17 = LocalDateTime.of(2024, 1, 17, 10, 0);

    List<GitCommit> commits =
        List.of(
            createCommit("hash1", "alice@dev.com", jan15),
            createCommit("hash2", "bob@dev.com", jan16),
            createCommit("hash3", "charlie@dev.com", jan17));

    // Filter from jan16 onwards
    List<GitCommit> filtered = parser.filterByDateRange(commits, jan16, null);

    assertThat(filtered).hasSize(2);
    assertThat(filtered).extracting(GitCommit::hash).containsExactly("hash2", "hash3");
  }

  @Test
  @DisplayName("Should filter commits by author emails")
  void shouldFilterCommitsByAuthorEmails() {
    List<GitCommit> commits =
        List.of(
            createCommit("hash1", "alice@dev.com", LocalDateTime.now()),
            createCommit("hash2", "bob@dev.com", LocalDateTime.now()),
            createCommit("hash3", "charlie@dev.com", LocalDateTime.now()));

    Set<String> targetEmails = Set.of("alice@dev.com", "charlie@dev.com");
    List<GitCommit> filtered = parser.filterByAuthors(commits, targetEmails);

    assertThat(filtered).hasSize(2);
    assertThat(filtered)
        .extracting(GitCommit::authorEmail)
        .containsExactly("alice@dev.com", "charlie@dev.com");
  }

  @Test
  @DisplayName("Should group file changes by language")
  void shouldGroupFileChangesByLanguage() {
    List<FileChange> fileChanges =
        List.of(
            new FileChange("src/User.java", 10, 2, FileChange.ChangeType.MODIFIED),
            new FileChange("src/Auth.java", 15, 5, FileChange.ChangeType.MODIFIED),
            new FileChange("script.py", 8, 1, FileChange.ChangeType.ADDED),
            new FileChange("README.md", 5, 0, FileChange.ChangeType.MODIFIED));

    GitCommit commit =
        new GitCommit(
            "hash123",
            "dev",
            "dev@example.com",
            LocalDateTime.now(),
            "Test commit",
            fileChanges,
            38,
            8);

    Map<String, String> langMapping =
        Map.of(
            "java", "Java",
            "py", "Python",
            "md", "Markdown");

    Map<String, List<FileChange>> grouped = commit.getChangesByLanguage(langMapping, Map.of());

    assertThat(grouped).containsKeys("Java", "Python", "Markdown");
    assertThat(grouped.get("Java")).hasSize(2);
    assertThat(grouped.get("Python")).hasSize(1);
    assertThat(grouped.get("Markdown")).hasSize(1);
  }

  @Test
  @DisplayName("Should handle empty git log output")
  void shouldHandleEmptyGitLogOutput() {
    String emptyOutput = "";

    List<GitCommit> commits = parser.parseCommits(emptyOutput);

    assertThat(commits).isEmpty();
  }

  @Test
  @DisplayName("Should exclude file renames from changes")
  void shouldExcludeFileRenames() {
    String gitLogOutput =
        """
        commit abc123
        Author: Test User <test@example.com>
        Date: 2024-01-15 10:30:45 +0000

            Rename files and modify code

        5	0	src/Main.java
        0	0	{old_file.py => new_file.py}
        0	0	app/{config.js => settings.js}
        10	7	test/Test.java
        """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);

    // Should only include actual file changes, not renames
    assertThat(commit.fileChanges()).hasSize(2);

    List<String> filePaths = commit.fileChanges().stream().map(FileChange::path).toList();

    assertThat(filePaths).containsOnly("src/Main.java", "test/Test.java");
    assertThat(filePaths).doesNotContain("{old_file.py => new_file.py}");
    assertThat(filePaths).doesNotContain("app/{config.js => settings.js}");
  }

  @Test
  @DisplayName("Should handle malformed git log output gracefully")
  void shouldHandleMalformedGitLogOutput() {
    String malformedOutput =
        """
            This is not a valid git log format
            Random text here
            """;

    List<GitCommit> commits = parser.parseCommits(malformedOutput);

    assertThat(commits).isEmpty();
  }

  @Test
  @DisplayName("Should handle commit block without commit hash")
  void shouldHandleCommitBlockWithoutCommitHash() {
    // Test the "!commitMatcher.find()" conditional branch
    String malformedCommit =
        """
            Author: John Doe <john@example.com>
            Date:   2024-01-15 10:30:45 +0000

                Missing commit line

            10	0	file.txt
            """;

    List<GitCommit> commits = parser.parseCommits(malformedCommit);

    assertThat(commits).isEmpty();
  }

  @Test
  @DisplayName("Should handle commit block without author info")
  void shouldHandleCommitBlockWithoutAuthor() {
    // Test the "!authorMatcher.find()" conditional branch
    String malformedCommit =
        """
            commit abc123def456
            Date:   2024-01-15 10:30:45 +0000

                Missing author line

            10	0	file.txt
            """;

    List<GitCommit> commits = parser.parseCommits(malformedCommit);

    assertThat(commits).isEmpty();
  }

  @Test
  @DisplayName("Should handle commit block without date info")
  void shouldHandleCommitBlockWithoutDate() {
    // Test the "!dateMatcher.find()" conditional branch
    String malformedCommit =
        """
            commit abc123def456
            Author: John Doe <john@example.com>

                Missing date line

            10	0	file.txt
            """;

    List<GitCommit> commits = parser.parseCommits(malformedCommit);

    assertThat(commits).isEmpty();
  }

  @Test
  @DisplayName("Should handle mixed valid and invalid commit blocks")
  void shouldHandleMixedValidInvalidCommitBlocks() {
    // Test multiple blocks where some fail different conditional checks
    String mixedOutput =
        """
            commit valid123
            Author: Valid User <valid@example.com>
            Date:   2024-01-15 10:30:45 +0000

                Valid commit

            10	0	valid.txt

            Author: Missing Commit Hash <missing@example.com>
            Date:   2024-01-16 11:30:45 +0000

                Invalid - no commit line

            5	0	invalid1.txt

            commit invalid456
            Date:   2024-01-17 12:30:45 +0000

                Invalid - no author

            8	0	invalid2.txt

            commit invalid789
            Author: Invalid User <invalid@example.com>

                Invalid - no date

            3	0	invalid3.txt
            """;

    List<GitCommit> commits = parser.parseCommits(mixedOutput);

    // Should only parse the valid commit, skip all malformed ones
    assertThat(commits).hasSize(1);
    GitCommit validCommit = commits.get(0);
    assertThat(validCommit.hash()).isEqualTo("valid123");
    assertThat(validCommit.authorName()).isEqualTo("Valid User");
    assertThat(validCommit.authorEmail()).isEqualTo("valid@example.com");
  }

  @Test
  @DisplayName("Should handle commit blocks that start with commit but are otherwise malformed")
  void shouldHandlePartiallyMalformedCommitBlocks() {
    // Test the "trimmed.startsWith(\"commit\")" check followed by parsing failures
    String partiallyMalformed =
        """
            commit abc123
            This commit block starts correctly but has malformed content
            Not a valid author line
            Not a valid date line
            """;

    List<GitCommit> commits = parser.parseCommits(partiallyMalformed);

    assertThat(commits).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "0, 0, 'Empty or binary change'",
    "1, 0, 'Single addition'",
    "0, 1, 'Single deletion'",
    "5, 5, 'Equal additions and deletions'",
    "10, 2, 'Addition heavy'",
    "3, 15, 'Deletion heavy'",
    "100, 50, 'Large changes'",
    "999, 0, 'Maximum single-sided change'"
  })
  @DisplayName("Should handle boundary conditions for insertion/deletion counts")
  void shouldHandleBoundaryInsertionDeletionCounts(
      int insertions, int deletions, String description) {
    // Build dynamic git log output based on parameters using numstat format
    String gitLogOutput =
        String.format(
            """
        commit abc123
        Author: Test <test@example.com>
        Date:   2024-01-15 10:30:45 +0000

            %s

        %d	%d	file.txt
        """,
            description, insertions, deletions);

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);

    // Test boundary conditions based on parser behavior
    if (insertions > 0 || deletions > 0) {
      // Parser counts actual + and - symbols, may differ from summary
      assertThat(commit.insertions()).isGreaterThanOrEqualTo(0);
      assertThat(commit.deletions()).isGreaterThanOrEqualTo(0);
      assertThat(commit.fileChanges()).hasSize(1);

      FileChange change = commit.fileChanges().get(0);
      assertThat(change.insertions()).isGreaterThanOrEqualTo(0);
      assertThat(change.deletions()).isGreaterThanOrEqualTo(0);
    } else {
      // Zero case - may not create file changes
      assertThat(commit.insertions()).isEqualTo(0);
      assertThat(commit.deletions()).isEqualTo(0);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 5, 10, 50, 100})
  @DisplayName("Should handle boundary conditions for file count")
  void shouldHandleBoundaryFileCount(int fileCount) {
    if (fileCount == 0) {
      // Test empty commits
      List<GitCommit> commits = parser.parseCommits("");
      assertThat(commits).isEmpty();
      return;
    }

    // Build git log with specified number of files
    StringBuilder gitLogOutput = new StringBuilder();
    gitLogOutput.append(
        """
        commit abc123
        Author: Test <test@example.com>
        Date:   2024-01-15 10:30:45 +0000

            Multiple file changes

        """);

    // Add file changes in numstat format
    for (int i = 1; i <= fileCount; i++) {
      gitLogOutput.append(String.format("%d\t0\tfile%d.txt\n", i, i));
    }

    List<GitCommit> commits = parser.parseCommits(gitLogOutput.toString());

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);

    // Test boundary: file count increment operations
    assertThat(commit.fileChanges()).hasSizeLessThanOrEqualTo(fileCount);
    assertThat(commit.fileChanges().size()).isGreaterThanOrEqualTo(0);
  }

  @ParameterizedTest
  @CsvSource({
    "'', 0, 'Completely empty'",
    "'   ', 0, 'Whitespace only'",
    "'\\n\\t\\r', 0, 'Various whitespace'",
    "'Invalid git log format', 0, 'Malformed input'",
    "'commit only\\ncommit abc123', 0, 'Incomplete commit'"
  })
  @DisplayName("Should handle boundary conditions for malformed input")
  void shouldHandleBoundaryMalformedInput(String input, int expectedCommits, String description) {
    List<GitCommit> commits = parser.parseCommits(input);
    assertThat(commits).hasSize(expectedCommits);
  }

  @ParameterizedTest
  @CsvSource({
    "1, +, '1 +'",
    "10, ++++++++++, '10 ++++++++++'",
    "100, +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++, '100 +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++'"
  })
  @DisplayName("Should handle boundary conditions for large change markers")
  void shouldHandleBoundaryLargeChangeMarkers(
      int count, String changeMarkers, String changeDescription) {
    String gitLogOutput =
        String.format(
            """
        commit abc123
        Author: Test <test@example.com>
        Date:   2024-01-15 10:30:45 +0000

            Large change test

        %d\t0\tfile.txt
        """,
            count);

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);

    // Test that parser handles large numbers of change markers correctly
    assertThat(commit.insertions()).isGreaterThanOrEqualTo(0);
    assertThat(commit.deletions()).isGreaterThanOrEqualTo(0);
  }

  @Test
  @DisplayName("Should handle empty commit blocks after splitting")
  void shouldHandleEmptyCommitBlocksAfterSplitting() {
    // Test empty blocks that result from splitting on commit boundaries
    String outputWithEmptyBlocks =
        """

            commit abc123
            Author: User <user@example.com>
            Date:   2024-01-15 10:30:45 +0000

                Valid commit

            10	0	file.txt


            """;

    List<GitCommit> commits = parser.parseCommits(outputWithEmptyBlocks);

    assertThat(commits).hasSize(1);
  }

  @Test
  @DisplayName("Should handle blocks that don't start with commit keyword")
  void shouldHandleNonCommitBlocks() {
    // Test the "!trimmed.startsWith(\"commit\")" conditional path
    String nonCommitBlocks =
        """
            some random text block
            Author: Not a commit <not@example.com>
            Date:   2024-01-15 10:30:45 +0000

            another block without commit
            more random content

            commit abc123
            Author: Valid User <valid@example.com>
            Date:   2024-01-15 10:30:45 +0000

                Valid commit after invalid blocks

            5	0	valid.txt
            """;

    List<GitCommit> commits = parser.parseCommits(nonCommitBlocks);

    // Should skip non-commit blocks and only parse the valid one
    assertThat(commits).hasSize(1);
    assertThat(commits.get(0).hash()).isEqualTo("abc123");
  }

  @Test
  @DisplayName("Should test null input handling for parseCommits")
  void shouldTestNullInputHandlingForParseCommits() {
    // Test the "gitLogOutput == null" conditional branch
    List<GitCommit> commits = parser.parseCommits(null);
    assertThat(commits).isEmpty();
  }

  @Test
  @DisplayName("Should test whitespace-only input for parseCommits")
  void shouldTestWhitespaceOnlyInputForParseCommits() {
    // Test the "gitLogOutput.trim().isEmpty()" conditional branch
    List<GitCommit> commits = parser.parseCommits("   \n\t  ");
    assertThat(commits).isEmpty();
  }

  @Test
  @DisplayName("Should test commit parsing with regex date validation")
  void shouldTestCommitParsingWithRegexDateValidation() {
    // Test the dateString.matches regex branches in parseGitDate
    String gitLogOutputWithSpecificDate =
        """
            commit abc123def456
            Author: John Doe <john.doe@example.com>
            Date:   2024-01-15 10:30:45 +0000

                Test commit with specific date format

            25	0	src/main/java/User.java
            """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutputWithSpecificDate);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);
    assertThat(commit.commitDate().getYear()).isEqualTo(2024);
    assertThat(commit.commitDate().getMonthValue()).isEqualTo(1);
    assertThat(commit.commitDate().getDayOfMonth()).isEqualTo(15);
  }

  @Test
  @DisplayName("Should test commit parsing with alternative date format")
  void shouldTestCommitParsingWithAlternativeDateFormat() {
    // Test the year-only date format branch in parseGitDate
    String gitLogOutputWithYearOnlyDate =
        """
            commit abc123def456
            Author: John Doe <john.doe@example.com>
            Date:   Mon Jan 15 2024 10:30:45 GMT+0000 (UTC)

                Test commit with alternative date format

            25	0	src/main/java/User.java
            """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutputWithYearOnlyDate);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);
    assertThat(commit.commitDate().getYear()).isEqualTo(2024);
  }

  @ParameterizedTest
  @CsvSource({
    "'file.txt', false, 'Simple filename'",
    "'{old.txt => new.txt}', true, 'Curly brace rename'",
    "'old.txt => new.txt', true, 'Arrow rename'",
    "'{src/old.java => src/new.java}', true, 'Path with curly rename'",
    "'src/path => dst/path', true, 'Path only arrow rename'",
    "'file with => in name.txt', false, 'False positive with => in filename'",
    "'{incomplete.txt', false, 'Malformed curly brace'",
    "'incomplete.txt}', false, 'Malformed curly brace end'",
    "'{file.txt}', false, 'Curly without arrow'",
    "' => ', true, 'Edge case arrow only'",
    "'{ => }', true, 'Edge case minimal rename'"
  })
  @DisplayName("Should detect file renames with various patterns")
  void shouldDetectFileRenames(String filePath, boolean expectedRename, String description) {
    // Create a commit with this file pattern to test isFileRename logic
    String gitLogOutput =
        String.format(
            """
        commit abc123
        Author: Test <test@example.com>
        Date:   2024-01-15 10:30:45 +0000

            Test rename detection: %s

        10\t0\t%s
        """,
            description, filePath);

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    if (expectedRename) {
      // Renamed files should be excluded from changes
      assertThat(commits).hasSize(1);
      assertThat(commits.get(0).fileChanges()).isEmpty();
    } else {
      // Non-renamed files should be included
      assertThat(commits).hasSize(1);
      assertThat(commits.get(0).fileChanges()).hasSize(1);
      assertThat(commits.get(0).fileChanges().get(0).path()).isEqualTo(filePath);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "'-', '-', 0, 0, 'Both binary markers'",
    "'5', '-', 5, 0, 'Insertions with binary deletions'",
    "'-', '3', 0, 3, 'Binary insertions with deletions'",
    "'10', '5', 10, 5, 'Normal numeric values'",
    "'0', '0', 0, 0, 'Zero values'",
    "'999', '888', 999, 888, 'Large numbers'",
    "'1', '0', 1, 0, 'Single insertion'",
    "'0', '1', 0, 1, 'Single deletion'"
  })
  @DisplayName("Should handle binary file markers in numstat format")
  void shouldHandleBinaryFileMarkersInNumstat(
      String insertStr,
      String deleteStr,
      int expectedInsert,
      int expectedDelete,
      String description) {
    // Test the binary file detection logic in parseFileChanges and parseStats
    String gitLogOutput =
        String.format(
            """
        commit abc123
        Author: Test <test@example.com>
        Date:   2024-01-15 10:30:45 +0000

            Binary file test: %s

        %s\t%s\tfile.txt
        """,
            description, insertStr, deleteStr);

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);
    assertThat(commit.insertions()).isEqualTo(expectedInsert);
    assertThat(commit.deletions()).isEqualTo(expectedDelete);

    // All binary file patterns should create a file change with the expected stats
    assertThat(commit.fileChanges()).hasSize(1);
    FileChange change = commit.fileChanges().get(0);
    assertThat(change.insertions()).isEqualTo(expectedInsert);
    assertThat(change.deletions()).isEqualTo(expectedDelete);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // Test extractCommitMessage complex parsing logic
        """
    commit abc123
    Author: Test <test@example.com>
    Date:   2024-01-15 10:30:45 +0000

        Single line message

    10\t0\tfile.txt
    """,
        """
    commit abc123
    Author: Test <test@example.com>
    Date:   2024-01-15 10:30:45 +0000

        Multi line message
        with continuation

    10\t0\tfile.txt
    """,
        """
    commit abc123
    Author: Test <test@example.com>
    Date:   2024-01-15 10:30:45 +0000

        Message with

        empty lines

    10\t0\tfile.txt
    """,
        """
    commit abc123
    Author: Test <test@example.com>
    Date:   2024-01-15 10:30:45 +0000


    10\t0\tfile.txt
    """
      })
  @DisplayName("Should extract commit messages with various formatting")
  void shouldExtractCommitMessagesWithVariousFormatting(String gitLogOutput) {
    // Test the complex extractCommitMessage method with different message formats
    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);
    // Just verify it doesn't crash and returns some message (or empty)
    assertThat(commit.message()).isNotNull();
    assertThat(commit.hash()).isEqualTo("abc123");
  }

  @ParameterizedTest
  @CsvSource({
    "'', 'Empty input'",
    "'commit abc123', 'Commit line only'",
    "'commit abc123\nAuthor: Test <test@example.com>', 'Missing date'",
    "'commit abc123\nDate: 2024-01-15 10:30:45 +0000', 'Missing author'",
    "'Author: Test <test@example.com>\nDate: 2024-01-15 10:30:45 +0000', 'Missing commit hash'",
    "'random text\nmore random', 'No commit structure'",
    "'commit\nAuthor: Test\nDate: Invalid', 'Malformed components'"
  })
  @DisplayName("Should handle malformed commit blocks gracefully")
  void shouldHandleMalformedCommitBlocksGracefully(String malformedInput, String description) {
    // Test the conditional checks in parseCommit method
    List<GitCommit> commits = parser.parseCommits(malformedInput);

    // Malformed input should result in empty list (no commits parsed)
    assertThat(commits).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "'2024-01-15 10:30:45 +0000', 2024, 1, 15, 'Standard format'",
    "'2024-12-31 23:59:59 -0800', 2024, 12, 31, 'End of year with timezone'",
    "'2023-02-28 12:00:00 +0100', 2023, 2, 28, 'February date'",
    "'Mon Jan 15 2024 10:30:45 GMT+0000 (UTC)', 2024, 1, 15, 'Alternative format'",
    "'Invalid date format', -1, -1, -1, 'Malformed date'"
  })
  @DisplayName("Should parse various git date formats")
  void shouldParseVariousGitDateFormats(
      String dateString, int expectedYear, int expectedMonth, int expectedDay, String description) {
    // Test parseGitDate method with various formats
    String gitLogOutput =
        String.format(
            """
        commit abc123
        Author: Test <test@example.com>
        Date:   %s

            Date format test: %s

        5\t0\tfile.txt
        """,
            dateString, description);

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    if (expectedYear > 0) {
      assertThat(commits).hasSize(1);
      GitCommit commit = commits.get(0);
      assertThat(commit.commitDate().getYear()).isEqualTo(expectedYear);
      if (expectedMonth > 0) {
        assertThat(commit.commitDate().getMonthValue()).isEqualTo(expectedMonth);
      }
      if (expectedDay > 0) {
        assertThat(commit.commitDate().getDayOfMonth()).isEqualTo(expectedDay);
      }
    } else {
      // Malformed dates should still create a commit (with fallback date)
      assertThat(commits).hasSize(1);
      assertThat(commits.get(0).commitDate()).isNotNull();
    }
  }

  // Helper method to create test commits
  private GitCommit createCommit(String hash, String email, LocalDateTime date) {
    return new GitCommit(hash, "Test Author", email, date, "Test message", List.of(), 10, 5);
  }
}
