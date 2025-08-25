package com.codestats.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.analysis.TeamComposition.ContributorImpact;
import com.codestats.analysis.TeamComposition.ContributorProfile;
import com.codestats.analysis.TeamComposition.ContributorRole;

class TeamCompositionTest {

  @Test
  @DisplayName("Should filter high impact contributors correctly")
  void shouldFilterHighImpactContributors() {
    ContributorProfile highImpact1 = createContributor("Alice", ContributorImpact.HIGH);
    ContributorProfile highImpact2 = createContributor("Bob", ContributorImpact.HIGH);
    ContributorProfile mediumImpact = createContributor("Charlie", ContributorImpact.MEDIUM);
    ContributorProfile lowImpact = createContributor("David", ContributorImpact.LOW);

    TeamComposition team =
        new TeamComposition(
            List.of(highImpact1, highImpact2, mediumImpact, lowImpact),
            4,
            3,
            "Team recommendation");

    List<ContributorProfile> highImpactContributors = team.getHighImpactContributors();

    assertThat(highImpactContributors).hasSize(2);
    assertThat(highImpactContributors).containsExactlyInAnyOrder(highImpact1, highImpact2);
  }

  @Test
  @DisplayName("Should provide team health indicator for at-risk team")
  void shouldProvideTeamHealthIndicatorForAtRiskTeam() {
    ContributorProfile singleHighImpact = createContributor("Solo Lead", ContributorImpact.HIGH);
    ContributorProfile lowImpact1 = createContributor("Junior 1", ContributorImpact.LOW);
    ContributorProfile lowImpact2 = createContributor("Junior 2", ContributorImpact.LOW);

    TeamComposition smallTeamWithOneLeader =
        new TeamComposition(
            List.of(singleHighImpact, lowImpact1, lowImpact2), 3, 2, "Small team recommendation");

    assertThat(smallTeamWithOneLeader.getTeamHealthIndicator())
        .isEqualTo("⚠️ Team at risk - single high-impact contributor");
  }

  @Test
  @DisplayName("Should provide team health indicator for healthy team")
  void shouldProvideTeamHealthIndicatorForHealthyTeam() {
    ContributorProfile highImpact1 = createContributor("Lead 1", ContributorImpact.HIGH);
    ContributorProfile highImpact2 = createContributor("Lead 2", ContributorImpact.HIGH);
    ContributorProfile mediumImpact = createContributor("Senior Dev", ContributorImpact.MEDIUM);
    ContributorProfile lowImpact = createContributor("Junior Dev", ContributorImpact.LOW);

    TeamComposition healthyTeam =
        new TeamComposition(
            List.of(highImpact1, highImpact2, mediumImpact, lowImpact),
            4,
            4,
            "Healthy team recommendation");

    assertThat(healthyTeam.getTeamHealthIndicator()).isEqualTo("✅ Healthy team structure");
  }

  @Test
  @DisplayName("Should provide team health indicator for developing team")
  void shouldProvideTeamHealthIndicatorForDevelopingTeam() {
    ContributorProfile mediumImpact1 = createContributor("Dev 1", ContributorImpact.MEDIUM);
    ContributorProfile mediumImpact2 = createContributor("Dev 2", ContributorImpact.MEDIUM);
    ContributorProfile lowImpact = createContributor("Junior", ContributorImpact.LOW);

    TeamComposition developingTeam =
        new TeamComposition(
            List.of(mediumImpact1, mediumImpact2, lowImpact),
            3,
            3,
            "Developing team recommendation");

    assertThat(developingTeam.getTeamHealthIndicator())
        .isEqualTo("📊 Developing team - needs senior leadership");
  }

  @Test
  @DisplayName("Should handle edge case with no high impact contributors")
  void shouldHandleNoHighImpactContributors() {
    ContributorProfile mediumImpact = createContributor("Medium Dev", ContributorImpact.MEDIUM);
    ContributorProfile lowImpact = createContributor("Low Dev", ContributorImpact.LOW);

    TeamComposition team =
        new TeamComposition(List.of(mediumImpact, lowImpact), 2, 2, "Team recommendation");

    assertThat(team.getHighImpactContributors()).isEmpty();
    assertThat(team.getTeamHealthIndicator())
        .isEqualTo("📊 Developing team - needs senior leadership");
  }

  @Test
  @DisplayName("Should handle large team with multiple high impact contributors")
  void shouldHandleLargeTeamWithMultipleHighImpact() {
    List<ContributorProfile> contributors =
        List.of(
            createContributor("Lead 1", ContributorImpact.HIGH),
            createContributor("Lead 2", ContributorImpact.HIGH),
            createContributor("Lead 3", ContributorImpact.HIGH),
            createContributor("Senior 1", ContributorImpact.MEDIUM),
            createContributor("Senior 2", ContributorImpact.MEDIUM),
            createContributor("Dev 1", ContributorImpact.LOW),
            createContributor("Dev 2", ContributorImpact.LOW));

    TeamComposition largeTeam =
        new TeamComposition(contributors, 7, 7, "Large team recommendation");

    assertThat(largeTeam.getHighImpactContributors()).hasSize(3);
    assertThat(largeTeam.getTeamHealthIndicator()).isEqualTo("✅ Healthy team structure");
  }

  @Test
  @DisplayName("Should test ContributorRole enum functionality")
  void shouldTestContributorRoleEnum() {
    assertThat(ContributorRole.TECH_LEAD.getDisplayName()).isEqualTo("Tech Lead");
    assertThat(ContributorRole.SENIOR_DEVELOPER.getDisplayName()).isEqualTo("Senior Developer");
    assertThat(ContributorRole.DEVELOPER.getDisplayName()).isEqualTo("Developer");
    assertThat(ContributorRole.JUNIOR_DEVELOPER.getDisplayName()).isEqualTo("Junior Developer");
    assertThat(ContributorRole.SPECIALIST.getDisplayName()).isEqualTo("Specialist");
    assertThat(ContributorRole.CONTRIBUTOR.getDisplayName()).isEqualTo("Contributor");
  }

  @Test
  @DisplayName("Should test ContributorImpact enum functionality")
  void shouldTestContributorImpactEnum() {
    assertThat(ContributorImpact.HIGH.getLabel()).isEqualTo("⭐ High Impact");
    assertThat(ContributorImpact.HIGH.getDescription())
        .isEqualTo("Drives major features and architecture");

    assertThat(ContributorImpact.MEDIUM.getLabel()).isEqualTo("📊 Medium Impact");
    assertThat(ContributorImpact.MEDIUM.getDescription())
        .isEqualTo("Contributes to features and maintenance");

    assertThat(ContributorImpact.LOW.getLabel()).isEqualTo("📝 Low Impact");
    assertThat(ContributorImpact.LOW.getDescription())
        .isEqualTo("Supports specific areas or occasional contributions");
  }

  @Test
  @DisplayName("Should handle team composition with different active member counts")
  void shouldHandleTeamCompositionWithDifferentActiveMemberCounts() {
    List<ContributorProfile> contributors =
        List.of(
            createContributor("Active 1", ContributorImpact.HIGH),
            createContributor("Active 2", ContributorImpact.MEDIUM));

    // Total members > active members (some inactive contributors)
    TeamComposition partiallyActiveTeam =
        new TeamComposition(contributors, 5, 2, "Partially active team");

    assertThat(partiallyActiveTeam.totalMembers()).isEqualTo(5);
    assertThat(partiallyActiveTeam.activeMembers()).isEqualTo(2);
    assertThat(partiallyActiveTeam.contributors()).hasSize(2);
  }

  @Test
  @DisplayName(
      "Should handle boundary case with exactly one high impact contributor in larger team")
  void shouldHandleBoundaryOneHighImpactInLargerTeam() {
    List<ContributorProfile> contributors =
        List.of(
            createContributor("Solo Lead", ContributorImpact.HIGH),
            createContributor("Dev 1", ContributorImpact.MEDIUM),
            createContributor("Dev 2", ContributorImpact.MEDIUM),
            createContributor("Dev 3", ContributorImpact.LOW),
            createContributor("Dev 4", ContributorImpact.LOW),
            createContributor("Dev 5", ContributorImpact.LOW));

    // 6 total members with 1 high impact (should not trigger "at risk" warning)
    TeamComposition largerTeamSingleLead =
        new TeamComposition(contributors, 6, 6, "Larger team single lead");

    assertThat(largerTeamSingleLead.getTeamHealthIndicator())
        .isEqualTo("📊 Developing team - needs senior leadership");
  }

  @Test
  @DisplayName(
      "Should handle boundary case with exactly 5 team members and 1 high impact contributor")
  void shouldHandleBoundaryExactlyFiveMembersOneHighImpact() {
    List<ContributorProfile> contributors =
        List.of(
            createContributor("Solo Lead", ContributorImpact.HIGH),
            createContributor("Dev 1", ContributorImpact.MEDIUM),
            createContributor("Dev 2", ContributorImpact.LOW),
            createContributor("Dev 3", ContributorImpact.LOW),
            createContributor("Dev 4", ContributorImpact.LOW));

    // Exactly 5 total members with 1 high impact (boundary condition)
    // Should NOT trigger "at risk" warning because totalMembers >= 5
    TeamComposition exactlyFiveMembers =
        new TeamComposition(contributors, 5, 5, "Exactly 5 member team");

    assertThat(exactlyFiveMembers.getTeamHealthIndicator())
        .isEqualTo("📊 Developing team - needs senior leadership");
  }

  // Helper method to create test contributors
  private ContributorProfile createContributor(String name, ContributorImpact impact) {
    return new ContributorProfile(
        name,
        name.toLowerCase().replace(" ", ".") + "@company.com",
        impact == ContributorImpact.HIGH ? 50 : (impact == ContributorImpact.MEDIUM ? 25 : 10),
        impact == ContributorImpact.HIGH
            ? 50.0
            : (impact == ContributorImpact.MEDIUM ? 25.0 : 10.0),
        impact == ContributorImpact.HIGH
            ? 10000
            : (impact == ContributorImpact.MEDIUM ? 5000 : 1000),
        impact == ContributorImpact.HIGH
            ? ContributorRole.TECH_LEAD
            : (impact == ContributorImpact.MEDIUM
                ? ContributorRole.SENIOR_DEVELOPER
                : ContributorRole.DEVELOPER),
        impact,
        List.of("Java", "JavaScript"));
  }
}
