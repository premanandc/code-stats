package com.codestats.output;

import com.codestats.service.CodeStatsService;

/**
 * Interface for formatting code statistics output.
 */
public interface OutputFormatter {
    
    /**
     * Format the analysis result for display.
     * 
     * @param result Analysis result to format
     * @return Formatted string output
     */
    String format(CodeStatsService.CodeStatsResult result);
}