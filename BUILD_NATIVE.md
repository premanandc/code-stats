# Building Native Executable with GraalVM

This project supports building native executables using GraalVM for faster startup and smaller memory footprint.

## Prerequisites

1. **Install GraalVM**:
   ```bash
   # Using SDKMAN (recommended)
   sdk install java 17.0.13-graal
   sdk use java 17.0.13-graal
   
   # Or download from https://www.graalvm.org/downloads/
   ```

2. **Install native-image component**:
   ```bash
   gu install native-image
   ```

## Building Native Executable

### Local Build
```bash
# Build native executable (requires GraalVM)
./mvnw clean package -Pnative

# The executable will be created at: target/code-stats
```

### Using the executable
```bash
# Run the native executable
./target/code-stats --help

# Example usage
./target/code-stats --repository /path/to/repo --output json
```

## Configuration

- **Main class**: `com.codestats.Main`
- **Executable name**: `code-stats`
- **Build profile**: `native`

## Native Image Configuration

The following configurations are included:

### Build Arguments
- `--no-fallback`: Prevents fallback to JVM
- `--enable-url-protocols=https,http`: Enables HTTP/HTTPS protocols
- `-H:+ReportExceptionStackTraces`: Better error reporting
- `--initialize-at-build-time`: Pre-initialize logging frameworks

### Reflection Configuration
Located in `src/main/resources/META-INF/native-image/`:
- `reflect-config.json`: Reflection configuration for model classes
- `resource-config.json`: Resource inclusion patterns

## CI/CD

GitHub Actions automatically builds native executables on the main branch:
- Only runs after tests pass
- Uploads executable as build artifact
- Available for 30 days

## Performance Benefits

Native executables provide:
- **Instant startup** (~10ms vs ~1000ms JVM)
- **Lower memory usage** (~10MB vs ~50MB JVM)
- **No JVM required** on target machine
- **Smaller distribution size**

## Troubleshooting

### Common Issues

1. **Missing reflection configuration**: 
   - Add classes to `reflect-config.json`
   - Use `--verbose` flag to see what's missing

2. **Resource not found**:
   - Add resources to `resource-config.json`

3. **Build fails with class initialization errors**:
   - Add problematic classes to `--initialize-at-build-time`

### Debug Native Build
```bash
# Build with verbose output
./mvnw clean package -Pnative -Dverbose=true

# Generate reachability metadata (if needed)
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
     -jar target/code-stats-1.0.0.jar --help
```