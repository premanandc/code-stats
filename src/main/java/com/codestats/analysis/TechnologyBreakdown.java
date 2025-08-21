package com.codestats.analysis;

import java.util.List;
import java.util.Map;

/** Analysis of technology stack and investment distribution. */
public record TechnologyBreakdown(
    List<TechnologyEntry> technologies,
    Map<String, Integer> languageDistribution,
    String primaryStack,
    String architectureType,
    List<String> technologyRecommendations) {

  public record TechnologyEntry(
      String language,
      int linesOfCode,
      double percentage,
      TechnologyCategory category,
      String businessValue) {}

  public enum TechnologyCategory {
    BACKEND("Backend", "🔧", "Server-side logic and APIs"),
    FRONTEND("Frontend", "🎨", "User interface and experience"),
    MOBILE("Mobile", "📱", "Mobile application development"),
    DEVOPS("DevOps", "⚙️", "Infrastructure and deployment"),
    DATA("Data", "📊", "Data processing and analytics"),
    TESTING("Testing", "🧪", "Quality assurance and testing"),
    CONFIG("Configuration", "⚙️", "Setup and configuration files"),
    DOCUMENTATION("Documentation", "📚", "Project documentation");

    private final String name;
    private final String emoji;
    private final String description;

    TechnologyCategory(String name, String emoji, String description) {
      this.name = name;
      this.emoji = emoji;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getEmoji() {
      return emoji;
    }

    public String getDescription() {
      return description;
    }

    public String getDisplayName() {
      return emoji + " " + name;
    }
  }

  public List<TechnologyEntry> getTopTechnologies(int limit) {
    return technologies.stream()
        .sorted((a, b) -> Integer.compare(b.linesOfCode(), a.linesOfCode()))
        .limit(limit)
        .toList();
  }

  public String getStackMaturity() {
    boolean hasModernFrontend =
        technologies.stream()
            .anyMatch(t -> t.language().equals("TypeScript") || t.language().equals("React"));
    boolean hasRobustBackend =
        technologies.stream()
            .anyMatch(t -> t.language().equals("Python") || t.language().equals("Java"));
    boolean hasTestingFramework =
        technologies.stream().anyMatch(t -> t.category() == TechnologyCategory.TESTING);

    if (hasModernFrontend && hasRobustBackend && hasTestingFramework) {
      return "🚀 Modern, well-architected stack";
    } else if (hasRobustBackend && hasTestingFramework) {
      return "🏗️ Solid foundation with room for frontend modernization";
    }
    return "📊 Developing stack - opportunities for modernization";
  }
}
