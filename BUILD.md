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

# Run static security analysis (SAST)
./mvnw spotbugs:check

# Run dependency vulnerability scanning (SCA)
./mvnw dependency-check:check

# Create executable JAR
./mvnw package

# Full verification (compile, test, format check, SAST + SCA security checks, package)
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

## Dependency Security Analysis (SCA)

This project uses [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/) for Software Composition Analysis (SCA).

**Key commands:**
- `mvn dependency-check:check` - Run dependency vulnerability scanning
- `mvn dependency-check:update-only` - Update CVE database only
- `mvn dependency-check:aggregate` - Generate aggregated report

**Build Integration:**
- Dependency-Check runs automatically during `mvn verify`
- Build will fail if dependencies have CVSS score ≥ 7.0 (High severity)
- Scans against National Vulnerability Database (NVD) and other sources

**Vulnerability Coverage:**
- **CVE Database** - National Vulnerability Database scanning
- **CVSS Scoring** - Risk-based vulnerability prioritization
- **GitHub Security Advisories** - Additional vulnerability feeds
- **CISA Known Exploited Vulnerabilities** - Actively exploited CVEs
- **Dependency Analysis** - Transitive dependency scanning

**Security Actions Taken:**
- **JGit upgraded** from 6.10.0 → 7.3.0 (Fixed CVE-2025-4949)
- **CVSS Threshold** set to 7.0 (High severity) for build failures
- **Production focus** - test dependencies excluded from scanning

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

## Complete Security Stack

This project implements **Defense in Depth** with multiple security layers:

1. **SAST (Static Application Security Testing)** - SpotBugs + FindSecBugs
   - Analyzes source code for security vulnerabilities
   - 140+ security patterns including OWASP Top 10

2. **SCA (Software Composition Analysis)** - OWASP Dependency-Check
   - Scans dependencies for known CVE vulnerabilities
   - Continuous monitoring against NVD database

3. **Code Quality** - Spotless with Google Java Style
   - Consistent formatting reduces security bugs
   - Automated code quality enforcement

**Security Report Locations:**
- `target/spotbugs.xml` - SAST security report
- `target/dependency-check-report.html` - SCA vulnerability report
- Both reports generated during `mvn verify`

