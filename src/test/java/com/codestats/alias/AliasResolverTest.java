package com.codestats.alias;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.model.ContributorIdentity;
import com.codestats.model.GitCommit;

class AliasResolverTest {

  private AliasResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new SimpleAliasResolver();
  }

  @Test
  @DisplayName("Should resolve single contributor with one email")
  void shouldResolveSingleContributorOneEmail() {
    List<GitCommit> commits =
        List.of(
            createCommit("John Doe", "john@company.com"),
            createCommit("John Doe", "john@company.com"));

    Map<String, ContributorIdentity> identities = resolver.resolveIdentities(commits, null);

    assertThat(identities).hasSize(1);
    ContributorIdentity identity = identities.get("john@company.com");

    assertThat(identity.canonicalName()).isEqualTo("John Doe");
    assertThat(identity.primaryEmail()).isEqualTo("john@company.com");
    assertThat(identity.allEmails()).containsExactly("john@company.com");
    assertThat(identity.allNames()).containsExactly("John Doe");
  }

  @Test
  @DisplayName("Should resolve contributor with multiple emails via alias config")
  void shouldResolveContributorWithAliasConfig() {
    List<GitCommit> commits =
        List.of(
            createCommit("John Doe", "john@company.com"),
            createCommit("John D", "john@personal.com"),
            createCommit("John", "jdoe@contractor.com"));

    Map<String, Set<String>> aliasConfig =
        Map.of("john@company.com", Set.of("john@personal.com", "jdoe@contractor.com"));

    Map<String, ContributorIdentity> identities = resolver.resolveIdentities(commits, aliasConfig);

    assertThat(identities).hasSize(1);
    ContributorIdentity identity = identities.get("john@company.com");

    assertThat(identity.canonicalName()).isEqualTo("John Doe"); // Longest name
    assertThat(identity.primaryEmail()).isEqualTo("john@company.com"); // Canonical email
    assertThat(identity.allEmails())
        .containsExactlyInAnyOrder("john@company.com", "john@personal.com", "jdoe@contractor.com");
    assertThat(identity.allNames()).containsExactlyInAnyOrder("John Doe", "John D", "John");
  }

  @Test
  @DisplayName("Should handle real-world Birgitta example")
  void shouldHandleBirgittaExample() {
    List<GitCommit> commits =
        List.of(
            createCommit("Birgitta B", "bboeckel@thoughtworks.com"),
            createCommit("Birgitta B", "birgitta410@users.noreply.github.com"),
            createCommit("Birgitta B.", "birgitta410@users.noreply.github.com"),
            createCommit("Birgitta Boeckeler", "bboeckel@thoughtworks.com") // Most complete name
            );

    Map<String, Set<String>> aliasConfig =
        Map.of("bboeckel@thoughtworks.com", Set.of("birgitta410@users.noreply.github.com"));

    Map<String, ContributorIdentity> identities = resolver.resolveIdentities(commits, aliasConfig);

    assertThat(identities).hasSize(1);
    ContributorIdentity identity = identities.get("bboeckel@thoughtworks.com");

    assertThat(identity.canonicalName()).isEqualTo("Birgitta Boeckeler"); // Longest/most complete
    assertThat(identity.primaryEmail()).isEqualTo("bboeckel@thoughtworks.com");
    assertThat(identity.allEmails())
        .containsExactlyInAnyOrder(
            "bboeckel@thoughtworks.com", "birgitta410@users.noreply.github.com");
    assertThat(identity.allNames())
        .containsExactlyInAnyOrder("Birgitta B", "Birgitta B.", "Birgitta Boeckeler");
  }

  @Test
  @DisplayName("Should choose canonical name based on length and frequency")
  void shouldChooseCanonicalNameCorrectly() {
    List<String> names = List.of("John", "John Doe", "John D", "John Doe Smith");
    Map<String, Integer> emailCounts = Map.of(); // Not used in current logic

    String canonical = resolver.findCanonicalName(names, emailCounts);

    assertThat(canonical).isEqualTo("John Doe Smith"); // Longest name
  }

  @Test
  @DisplayName("Should choose primary email based on commit count")
  void shouldChoosePrimaryEmailByCommitCount() {
    Set<String> emails = Set.of("john@company.com", "john@personal.com");
    Map<String, Integer> emailCounts =
        Map.of(
            "john@company.com", 10, // More commits
            "john@personal.com", 3);

    String primary = resolver.findPrimaryEmail(emails, emailCounts);

    assertThat(primary).isEqualTo("john@company.com");
  }

  @Test
  @DisplayName("Should resolve email aliases correctly")
  void shouldResolveEmailAliases() {
    Map<String, Set<String>> aliasConfig =
        Map.of("john@company.com", Set.of("john@personal.com", "jdoe@contractor.com"));

    assertThat(resolver.resolveEmailAlias(aliasConfig, "john@company.com"))
        .isEqualTo("john@company.com"); // Canonical
    assertThat(resolver.resolveEmailAlias(aliasConfig, "john@personal.com"))
        .isEqualTo("john@company.com"); // Alias resolved
    assertThat(resolver.resolveEmailAlias(aliasConfig, "other@email.com"))
        .isEqualTo("other@email.com"); // No alias, returns self
  }

  @Test
  @DisplayName("Should handle multiple separate contributors")
  void shouldHandleMultipleContributors() {
    List<GitCommit> commits =
        List.of(
            createCommit("Alice Smith", "alice@company.com"),
            createCommit("Bob Jones", "bob@company.com"),
            createCommit("Alice", "alice@personal.com") // Different Alice or same?
            );

    Map<String, Set<String>> aliasConfig =
        Map.of("alice@company.com", Set.of("alice@personal.com"));

    Map<String, ContributorIdentity> identities = resolver.resolveIdentities(commits, aliasConfig);

    assertThat(identities).hasSize(2);

    ContributorIdentity alice = identities.get("alice@company.com");
    assertThat(alice.canonicalName()).isEqualTo("Alice Smith");
    assertThat(alice.allEmails())
        .containsExactlyInAnyOrder("alice@company.com", "alice@personal.com");

    ContributorIdentity bob = identities.get("bob@company.com");
    assertThat(bob.canonicalName()).isEqualTo("Bob Jones");
    assertThat(bob.allEmails()).containsExactly("bob@company.com");
  }

  @Test
  @DisplayName("Should create display string with aliases")
  void shouldCreateDisplayStringWithAliases() {
    ContributorIdentity identity =
        ContributorIdentity.builder()
            .canonicalName("Birgitta Boeckeler")
            .primaryEmail("bboeckel@thoughtworks.com")
            .allEmails(List.of("bboeckel@thoughtworks.com", "birgitta410@users.noreply.github.com"))
            .allNames(List.of("Birgitta Boeckeler", "Birgitta B"))
            .build();

    String display = identity.getDisplayString();

    assertThat(display).contains("Birgitta Boeckeler");
    assertThat(display).contains("birgitta410@users.noreply.github.com");
    assertThat(display).contains("also:");
  }

  @Test
  @DisplayName("Should handle no alias config gracefully")
  void shouldHandleNoAliasConfig() {
    List<GitCommit> commits =
        List.of(
            createCommit("John Doe", "john@company.com"),
            createCommit("Jane Smith", "jane@company.com"));

    Map<String, ContributorIdentity> identities = resolver.resolveIdentities(commits, null);

    assertThat(identities).hasSize(2);
    assertThat(identities.keySet())
        .containsExactlyInAnyOrder("john@company.com", "jane@company.com");
  }

  // Helper method to create test commits
  private GitCommit createCommit(String authorName, String authorEmail) {
    return new GitCommit(
        "hash123", authorName, authorEmail, LocalDateTime.now(), "Test commit", List.of(), 10, 5);
  }
}
