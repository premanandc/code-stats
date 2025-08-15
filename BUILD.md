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

# Create executable JAR
./mvnw package

# Full verification (compile, test, format check, package)
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