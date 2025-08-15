# Build Instructions

## Prerequisites

- Java 17+
- Maven (or use included Maven Wrapper)

## Building

```bash
# Build the project
./mvnw clean compile

# Run tests
./mvnw test

# Check code formatting
./mvnw spotless:check

# Auto-fix code formatting
./mvnw spotless:apply

# Run security analysis
./mvnw spotbugs:check

# Create executable JAR
./mvnw package

# Full verification (compile, test, format check, security check, package)
./mvnw clean verify
```

## Code Style

This project uses [Spotless](https://github.com/diffplug/spotless) with [Google Java Format](https://github.com/google/google-java-format) for consistent code formatting.

**Key commands:**
- `mvn spotless:check` - Check if code follows style guidelines
- `mvn spotless:apply` - Automatically fix formatting violations

**Build Integration:**
- Spotless runs automatically during `mvn verify`
- Build will fail if code doesn't follow style guidelines
- Use `mvn spotless:apply` to fix issues before committing

## Formatting Rules

- **Java**: Google Java Style with 2-space indentation
- **Import order**: java|javax, org, com, (custom packages)
- **POM files**: Sorted elements and properties
- **JSON/YAML**: Jackson formatting
- **Markdown**: Flexmark formatting

## Security Analysis

This project uses [SpotBugs](https://spotbugs.github.io/) with [FindSecBugs](https://find-sec-bugs.github.io/) for static security analysis.

**Key commands:**
- `mvn spotbugs:check` - Run security vulnerability analysis
- `mvn spotbugs:gui` - View detailed security report in GUI
- `mvn spotbugs:spotbugs` - Generate security report without failing build

**Build Integration:**
- SpotBugs runs automatically during `mvn verify`
- Build will fail if security vulnerabilities are found
- Security analysis focuses on OWASP Top 10 and CWE vulnerabilities

**Security Checks:**
- **SQL Injection** detection (Hibernate, JPA, JDBC)
- **Cross-Site Scripting (XSS)** vulnerabilities
- **Command Injection** prevention
- **Cryptographic issues** (weak algorithms, hardcoded keys)
- **Path Traversal** vulnerabilities
- **XML External Entity (XXE)** attacks
- **Information Exposure** through error messages
- **140+ security vulnerability patterns**

**Security Features Implemented:**
- Input sanitization for git command parameters
- Error message sanitization to prevent information exposure
- Command injection prevention with parameter validation

