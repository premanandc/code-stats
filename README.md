# Code Stats

A powerful git repository statistics tool for analyzing contributor activity, built with Java and Maven. Get detailed insights into commits, files changed, lines of code, and language-specific breakdowns.

## Features

- **Contributor Statistics** - Commits, files changed, lines added/deleted
- **Language Analysis** - Automatic detection with production vs test code separation
- **Date Filtering** - Analyze specific time periods with `--days`, `--since`, `--until`
- **Email Alias Resolution** - Handle contributors with multiple email addresses
- **Flexible Output** - Colored terminal output or JSON for automation
- **Single Executable** - Self-contained JAR file, no external dependencies
- **Configuration Files** - Customize language mappings and directory patterns

## Quick Start

### Download and Run

```bash
# Download the latest release
wget https://github.com/your-org/code-stats/releases/latest/code-stats-1.0.0.jar

# Run on current directory
java -jar code-stats-1.0.0.jar

# Run on specific repository
java -jar code-stats-1.0.0.jar /path/to/your/repo
```

### Example Output

```
📊 Code Statistics Report
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Repository: /path/to/your/project
Period: Last 30 days
Total commits: 45

Contributors:
1. Alice Developer
   Email: alice@company.com
   Commits: 25 (55.6%)
   Files changed: 127
   Lines: +2,341 -892 (net: +1,449)
   Languages: Java (1,205) JavaScript (634) JSON (102)
   Code distribution: Production: 1,789, Test: 552

2. Bob Contributor
   Email: bob@company.com
   Commits: 20 (44.4%)
   Files changed: 89
   Lines: +1,567 -234 (net: +1,333)
   Languages: Java (987) XML (345) Markdown (235)
   Code distribution: Production: 1,122, Test: 445
```

## Usage

### Basic Commands

```bash
# Analyze current directory
java -jar code-stats-1.0.0.jar

# Analyze specific repository
java -jar code-stats-1.0.0.jar /path/to/repo

# Last 7 days only
java -jar code-stats-1.0.0.jar --days 7

# Specific date range
java -jar code-stats-1.0.0.jar --since 2025-01-01 --until 2025-01-31

# JSON output for automation
java -jar code-stats-1.0.0.jar --json

# Include specific contributors
java -jar code-stats-1.0.0.jar --include-users alice@company.com,bob@company.com

# Exclude contributors
java -jar code-stats-1.0.0.jar --exclude-users automated@bot.com
```

### Configuration

Generate a sample configuration file:

```bash
java -jar code-stats-1.0.0.jar --init-config
```

This creates `code-stats-config.yaml` with customizable settings:

```yaml
# Language Detection
languages:
  js: JavaScript
  ts: TypeScript
  py: Python
  # Add your custom extensions

# Directory Classification  
productionDirectories:
  - src
  - lib
  - app

testDirectories:
  - test
  - tests
  - __tests__

# Email Aliases
aliases:
  "alice@company.com":
    - "alice@personal.com"
    - "a.developer@company.com"
```

Use with custom configuration:

```bash
java -jar code-stats-1.0.0.jar --config my-config.yaml
```

### Command Line Options

```
Usage: code-stats [OPTIONS] [REPOSITORY]

Arguments:
  REPOSITORY                Repository path (default: current directory)

Options:
  -d, --days=<days>        Number of days in the past to analyze
  --since=<date>           Start date (YYYY-MM-DD format)
  --until=<date>           End date (YYYY-MM-DD format)
  --include-users=<emails> Comma-separated list of user emails to include
  --exclude-users=<emails> Comma-separated list of user emails to exclude
  --config=<file>          Configuration file path
  --ext=<mappings>         Custom file extensions (ext1:Lang1,ext2:Lang2)
  --json                   Output results in JSON format
  --no-color               Disable colored output
  --init-config            Generate sample configuration file
  -h, --help               Show help message
  -V, --version            Show version information
```

## Installation

### Prerequisites

- Java 17 or later
- Git (accessible from command line)

### Option 1: Download Release

Download the latest JAR from the [releases page](https://github.com/your-org/code-stats/releases).

### Option 2: Build from Source

```bash
git clone https://github.com/your-org/code-stats.git
cd code-stats
./mvnw package
java -jar target/code-stats-1.0.0.jar
```

## Use Cases

### Development Teams

```bash
# Weekly team standup stats
java -jar code-stats-1.0.0.jar --days 7

# Sprint analysis
java -jar code-stats-1.0.0.jar --since 2025-01-15 --until 2025-01-29

# Individual contributor focus
java -jar code-stats-1.0.0.jar --include-users developer@company.com --days 30
```

### Project Management

```bash
# JSON output for dashboards
java -jar code-stats-1.0.0.jar --json --days 30 > stats.json

# Exclude bots and automation
java -jar code-stats-1.0.0.jar --exclude-users bot@github.com,ci@company.com

# Focus on production code changes
java -jar code-stats-1.0.0.jar --config production-only-config.yaml
```

### Code Reviews

```bash
# Recent changes by team member
java -jar code-stats-1.0.0.jar --include-users alice@company.com --days 3

# Language-specific analysis
java -jar code-stats-1.0.0.jar --ext "kt:Kotlin,swift:Swift" mobile-repo/
```

## Configuration Examples

### Custom Language Mapping

```yaml
languages:
  kt: Kotlin
  swift: Swift
  dart: Dart
  vue: "Vue.js"
  scss: "SCSS"
```

### Directory Patterns

```yaml
productionDirectories:
  - src/main
  - lib
  - packages
  
testDirectories:
  - src/test
  - test
  - spec
  - cypress
```

### Email Aliases

```yaml
aliases:
  "john.doe@company.com":
    - "john@personal.com"
    - "j.doe@company.com"
    - "John Doe <john.doe@company.com>"
```

## Contributing

See [BUILD.md](BUILD.md) for development setup, testing, and security analysis information.

### Quick Development Setup

```bash
git clone https://github.com/your-org/code-stats.git
cd code-stats
./mvnw clean verify
java -jar target/code-stats-1.0.0.jar --help
```

## Security

This project includes comprehensive security analysis:

- **SAST** - Static code analysis with SpotBugs + FindSecBugs
- **SCA** - Dependency vulnerability scanning with OWASP Dependency-Check
- **Security-first development** with automated vulnerability detection

## License

[MIT License](LICENSE) - see the LICENSE file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/your-org/code-stats/issues)
- **Documentation**: See [BUILD.md](BUILD.md) for detailed build instructions
- **Security**: Report security issues privately via email to security@your-org.com