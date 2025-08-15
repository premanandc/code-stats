package com.codestats.alias;

import com.codestats.model.ContributorIdentity;
import com.codestats.model.GitCommit;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Functional interface for resolving contributor aliases and determining canonical identities.
 */
public interface AliasResolver {
    
    /**
     * Resolve a list of commits to unified contributor identities.
     * Groups commits by contributor, handling email aliases and name variations.
     * 
     * @param commits List of git commits
     * @param aliasConfig Optional alias configuration from user
     * @return Map from canonical email to unified identity
     */
    Map<String, ContributorIdentity> resolveIdentities(List<GitCommit> commits, 
                                                       Map<String, Set<String>> aliasConfig);
    
    /**
     * Find the canonical name for a contributor given multiple name variations.
     * Strategy: Choose the longest/most complete name that appears most frequently.
     * 
     * @param names List of name variations for same contributor
     * @param emailCounts Map of email to commit count (for weighting)
     * @return Canonical name to display
     */
    default String findCanonicalName(List<String> names, Map<String, Integer> emailCounts) {
        return names.stream()
            .max((name1, name2) -> {
                // First preference: longer name (more complete)
                int lengthComparison = Integer.compare(name1.length(), name2.length());
                if (lengthComparison != 0) {
                    return lengthComparison;
                }
                
                // Second preference: alphabetical (for deterministic results)
                return name1.compareTo(name2);
            })
            .orElse("Unknown");
    }
    
    /**
     * Find the primary email for a contributor.
     * Strategy: Email with most commits, or first alphabetically if tied.
     * 
     * @param emails Set of emails for same contributor
     * @param emailCounts Map of email to commit count
     * @return Primary email to use
     */
    default String findPrimaryEmail(Set<String> emails, Map<String, Integer> emailCounts) {
        return emails.stream()
            .max((email1, email2) -> {
                // First preference: email with more commits
                int count1 = emailCounts.getOrDefault(email1, 0);
                int count2 = emailCounts.getOrDefault(email2, 0);
                int countComparison = Integer.compare(count1, count2);
                if (countComparison != 0) {
                    return countComparison;
                }
                
                // Second preference: alphabetical (for deterministic results)
                return email1.compareTo(email2);
            })
            .orElse("unknown@example.com");
    }
    
    /**
     * Apply user-defined alias rules to group emails together.
     * 
     * @param aliasConfig Map where key is canonical email, value is set of alias emails
     * @param email Email to resolve
     * @return Canonical email this email should map to
     */
    default String resolveEmailAlias(Map<String, Set<String>> aliasConfig, String email) {
        if (aliasConfig == null) {
            return email;
        }
        
        // Check if this email is a canonical email
        if (aliasConfig.containsKey(email)) {
            return email;
        }
        
        // Check if this email is an alias for any canonical email
        return aliasConfig.entrySet().stream()
            .filter(entry -> entry.getValue().contains(email))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(email);
    }
}