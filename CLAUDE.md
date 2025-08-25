# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Essential development commands
./mvnw clean verify                    # Full build with tests, formatting, security checks
./mvnw test                           # Run tests only
./mvnw spotless:apply                 # Auto-fix code formatting
./mvnw spotbugs:check                 # Security analysis (SAST)
./mvnw dependency-check:check         # Dependency vulnerability scan (SCA)

# Run the application
java -jar target/code-stats-1.0.0.jar [repository-path]
java -jar target/code-stats-1.0.0.jar --dashboard    # Executive dashboard output
java -jar target/code-stats-1.0.0.jar --json         # JSON output

# Testing
./mvnw test -Dtest=ClassName          # Run single test class
./mvnw test -Dtest=ClassName#method   # Run single test method
./mvnw jacoco:report                  # Generate code coverage report

# Mutation Testing (Pitest)
./mvnw org.pitest:pitest-maven:mutationCoverage -Pmutation-test -B  # Run mutation tests
# Current mutation coverage: 61% (433/707 mutations killed), Test strength: 64%
# Report available at: target/pit-reports/index.html
```

## Architecture Overview

The application follows a layered architecture with dependency injection and functional programming patterns:

**Core Flow**: Git Repository → Git Log Parser → Statistics Aggregator → Business Analyzer → Output Formatter

### Key Architectural Components

**Main Entry Point**: `com.codestats.Main` - Picocli command-line application that wires dependencies and orchestrates execution

**Service Layer**: 
- `CodeStatsService` - Main orchestration service that coordinates all components
- Dependency injection pattern with constructor injection
- Result object pattern with `CodeStatsService.CodeStatsResult`

**Git Analysis**:
- `GitLogParser` interface with `JGitLogParser` implementation using JGit library
- `GitCommandExecutor` interface with `ProcessGitCommandExecutor` for git operations
- Immutable data models: `GitCommit`, `FileChange` 

**Statistics Processing**:
- `StatisticsAggregator` interface with `SimpleStatisticsAggregator` implementation
- Functional interface design for flexible aggregation strategies
- Language detection through file extensions and filename patterns

**Business Intelligence**:
- `BusinessAnalyzer` - Transforms raw statistics into business insights
- Analysis models: `ProjectSummary`, `TeamComposition`, `RiskAssessment`, `QualityMetrics`, `TechnologyBreakdown`
- Executive-level insights including bus factor, risk assessment, technology categorization

**Output System**:
- `OutputFormatter` interface with three implementations:
  - `TextOutputFormatter` - Human-readable colored terminal output
  - `JsonOutputFormatter` - Machine-readable JSON for automation
  - `ExecutiveDashboardFormatter` - Business intelligence dashboard with ASCII charts
- Strategy pattern for pluggable output formats

**Configuration**:
- `CodeStatsConfig` record for YAML-based configuration
- Jackson YAML parsing with builder pattern
- Supports language mappings, directory patterns, email aliases, user filtering

**Data Models**:
- Immutable records throughout (`ContributorStats`, `LanguageStats`, `ContributorIdentity`)
- Functional approach with no mutable state
- Rich domain models with calculated properties

### Key Design Patterns

- **Strategy Pattern**: OutputFormatter implementations
- **Template Method**: StatisticsAggregator with default methods  
- **Builder Pattern**: Configuration merging and business analysis
- **Functional Interface**: Single-method interfaces for lambda support
- **Immutable Objects**: Records and final fields throughout
- **Dependency Injection**: Constructor-based wiring in Main

## Testing Architecture

Comprehensive test coverage with different test types:

**Unit Tests**: Each component has dedicated test class (e.g., `SimpleStatisticsAggregatorTest`)
**Integration Tests**: End-to-end testing through `CodeStatsServiceTest`
**Business Logic Tests**: `BusinessAnalyzerTest` with complex scenario testing
**Formatter Tests**: `ExecutiveDashboardFormatterTest` for output validation

Test utilities use builder patterns and fixture methods for creating test data with proper threshold compliance.

## Code Quality & Testing Standards

### Mutation Testing with Pitest
- **Current Status**: 61% mutation coverage (433/707 mutations killed), 64% test strength
- **Configuration**: Uses `mutation-test` profile with STRONGER mutator set
- **Target Threshold**: 60% minimum mutation coverage (currently passing)
- **Coverage Analysis**: Focus on packages with lower mutation coverage:
  - `com.codestats.analysis`: 54% (needs improvement)
  - `com.codestats.service`: 68% (good)
  - `com.codestats.alias`: 66% (acceptable)

### Testing Best Practices
- **Test Observable Behavior**: Write tests that assert on public method outputs and side effects, not internal implementation details
- **Avoid Testing Private Methods**: Focus on testing through public APIs
- **Use Meaningful Test Data**: Create realistic test scenarios that reflect actual usage patterns
- **Boundary Condition Testing**: Test edge cases like empty inputs, boundary values, and null parameters
- **Test Business Logic**: Ensure algorithms produce correct results for different input combinations
- **Integration Over Isolation**: Prefer testing component interactions over heavily mocked unit tests

### Test Naming Conventions
- Use descriptive test method names: `shouldCalculateCorrectRiskAssessmentForHighImpactTeam()`
- Follow pattern: `should[ExpectedBehavior]When[Condition]` or `should[ExpectedBehavior]For[Scenario]`
- Use `@DisplayName` annotations for complex scenarios

### Test Data Creation
- Use builder patterns and factory methods for test fixtures
- Create realistic data that matches business thresholds (e.g., TECH_LEAD requires ≥50 commits AND ≥10000 lines)
- Avoid magic numbers - use named constants or calculated values
- Test with edge cases: empty collections, boundary values, extreme scenarios

## Security Implementation

**SAST (Static Application Security Testing)**: SpotBugs with FindSecBugs plugin
- Configured in `spotbugs-security-exclude.xml` with justified suppressions
- 140+ security vulnerability patterns
- Command injection prevention in `ProcessGitCommandExecutor`

**SCA (Software Composition Analysis)**: OWASP Dependency-Check
- CVE scanning with CVSS ≥ 7.0 threshold
- Upgraded JGit to fix CVE-2025-4949

**Input Sanitization**: Git command parameter validation and error message sanitization

## Code Quality Standards

**Formatting**: Google Java Style with Spotless plugin (2-space indentation)
**Unicode Handling**: All `toLowerCase()` calls use `Locale.ROOT` for consistency
**Testing**: High code coverage with JaCoCo reporting and Pitest mutation testing
**Documentation**: Comprehensive JavaDoc for public APIs

### Quality Gates
1. **Line Coverage**: Minimum 70% (JaCoCo enforced)
2. **Mutation Coverage**: Minimum 60% (Pitest threshold)
3. **Code Formatting**: Must pass Spotless checks
4. **Security Analysis**: Must pass SpotBugs + FindSecBugs
5. **Dependency Security**: No CVEs with CVSS ≥ 7.0

### Continuous Quality Maintenance
- Run `./mvnw clean verify` before committing (includes all quality checks)
- Use Pitest reports to identify weak test areas: `target/pit-reports/index.html`
- Focus mutation testing improvements on business-critical packages
- Review survived mutations to identify missing test scenarios
- Maintain test-first approach for new features to prevent mutation coverage regression