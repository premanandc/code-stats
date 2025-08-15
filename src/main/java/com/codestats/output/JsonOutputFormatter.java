package com.codestats.output;

import com.codestats.service.CodeStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** JSON output formatter for programmatic consumption. */
public class JsonOutputFormatter implements OutputFormatter {

  private final ObjectMapper objectMapper;

  public JsonOutputFormatter() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
  }

  @Override
  public String format(CodeStatsService.CodeStatsResult result) {
    try {
      return objectMapper.writeValueAsString(result);
    } catch (Exception e) {
      return "{\"error\": \"Failed to serialize result to JSON: " + e.getMessage() + "\"}";
    }
  }
}
