package com.codestats;

import com.codestats.alias.SimpleAliasResolver;
import com.codestats.config.CodeStatsConfig;
import com.codestats.git.JGitLogParser;
import com.codestats.output.JsonOutputFormatter;
import com.codestats.output.OutputFormatter;
import com.codestats.output.TextOutputFormatter;
import com.codestats.service.CodeStatsService;
import com.codestats.service.ProcessGitCommandExecutor;
import com.codestats.stats.SimpleStatisticsAggregator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
    name = "code-stats",
    description = "Analyze git repository contributor statistics",
    version = "1.0.0",
    mixinStandardHelpOptions = true
)
public class Main implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Repository path (default: current directory)",
        defaultValue = "."
    )
    private File repository;

    @Option(
        names = {"-d", "--days"},
        description = "Number of days in the past to analyze"
    )
    private Integer days;

    @Option(
        names = {"--since"},
        description = "Start date (YYYY-MM-DD format)"
    )
    private String since;

    @Option(
        names = {"--until"},
        description = "End date (YYYY-MM-DD format)"
    )
    private String until;

    @Option(
        names = {"--include-users"},
        description = "Comma-separated list of user emails to include",
        split = ","
    )
    private String[] includeUsers;

    @Option(
        names = {"--exclude-users"},
        description = "Comma-separated list of user emails to exclude",
        split = ","
    )
    private String[] excludeUsers;

    @Option(
        names = {"--config"},
        description = "Configuration file path"
    )
    private File configFile;

    @Option(
        names = {"--json"},
        description = "Output results in JSON format"
    )
    private boolean jsonOutput;

    @Option(
        names = {"--ext"},
        description = "Custom file extensions mapping (format: ext1:Lang1,ext2:Lang2)"
    )
    private String customExtensions;

    @Option(
        names = {"--no-color"},
        description = "Disable colored output"
    )
    private boolean noColor;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try {
            // Initialize service components
            CodeStatsService service = new CodeStatsService(
                new JGitLogParser(),
                new SimpleAliasResolver(),
                new SimpleStatisticsAggregator(),
                new ProcessGitCommandExecutor()
            );
            
            // Load configuration
            CodeStatsConfig config = loadConfiguration();
            
            // Parse date filters
            LocalDateTime sinceDate = parseDate(since);
            LocalDateTime untilDate = parseDate(until);
            
            // Parse user filters
            Set<String> includeUserSet = parseUserList(includeUsers);
            Set<String> excludeUserSet = parseUserList(excludeUsers);
            
            // Build request
            CodeStatsService.CodeStatsRequest request = CodeStatsService.CodeStatsRequest.builder()
                .repositoryPath(repository)
                .config(config)
                .days(days)
                .since(sinceDate)
                .until(untilDate)
                .includeUsers(includeUserSet)
                .excludeUsers(excludeUserSet)
                .build();
            
            // Execute analysis
            CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);
            
            // Format and output results
            OutputFormatter formatter = jsonOutput 
                ? new JsonOutputFormatter()
                : new TextOutputFormatter(!noColor);
            
            System.out.println(formatter.format(result));
            
            return result.isSuccess() ? 0 : 1;
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (System.getProperty("debug") != null) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    private CodeStatsConfig loadConfiguration() {
        CodeStatsConfig config = CodeStatsConfig.getDefault();
        
        // TODO: Load from config file if specified
        // For now, just handle custom extensions
        if (customExtensions != null) {
            Map<String, String> customLangMap = parseCustomExtensions(customExtensions);
            config = config.mergeWith(new CodeStatsConfig(
                customLangMap, 
                config.productionDirectories(),
                config.testDirectories(),
                config.aliases()
            ));
        }
        
        return config;
    }
    
    private Map<String, String> parseCustomExtensions(String extensionsStr) {
        var customExtensions = new java.util.HashMap<String, String>();
        
        if (extensionsStr != null && !extensionsStr.trim().isEmpty()) {
            String[] pairs = extensionsStr.split(",");
            for (String pair : pairs) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    customExtensions.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        
        return Map.copyOf(customExtensions);
    }
    
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try parsing as date only (YYYY-MM-DD)
            LocalDate date = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr + ". Use YYYY-MM-DD format.");
        }
    }
    
    private Set<String> parseUserList(String[] users) {
        if (users == null || users.length == 0) {
            return Set.of();
        }
        
        return Set.of(users);
    }
}