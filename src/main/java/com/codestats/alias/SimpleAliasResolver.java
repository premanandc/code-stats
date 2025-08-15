package com.codestats.alias;

import com.codestats.model.ContributorIdentity;
import com.codestats.model.GitCommit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple implementation of AliasResolver using functional programming principles.
 * Pure functions, immutable data structures, no side effects.
 */
public class SimpleAliasResolver implements AliasResolver {
    
    @Override
    public Map<String, ContributorIdentity> resolveIdentities(List<GitCommit> commits, 
                                                              Map<String, Set<String>> aliasConfig) {
        if (commits == null || commits.isEmpty()) {
            return Map.of();
        }
        
        // Step 1: Group commits by resolved canonical email
        Map<String, List<GitCommit>> commitsByCanonicalEmail = commits.stream()
            .collect(Collectors.groupingBy(
                commit -> resolveEmailAlias(aliasConfig, commit.authorEmail())
            ));
        
        // Step 2: Build contributor identities for each canonical email
        Map<String, ContributorIdentity> identities = new HashMap<>();
        
        for (Map.Entry<String, List<GitCommit>> entry : commitsByCanonicalEmail.entrySet()) {
            String canonicalEmail = entry.getKey();
            List<GitCommit> contributorCommits = entry.getValue();
            
            ContributorIdentity identity = buildIdentityFromCommits(canonicalEmail, contributorCommits, aliasConfig);
            identities.put(canonicalEmail, identity);
        }
        
        return Map.copyOf(identities);
    }
    
    /**
     * Build a ContributorIdentity from a list of commits for the same person.
     */
    private ContributorIdentity buildIdentityFromCommits(String canonicalEmail, 
                                                         List<GitCommit> commits,
                                                         Map<String, Set<String>> aliasConfig) {
        // Collect all emails and names used by this contributor
        Set<String> allEmails = new HashSet<>();
        Set<String> allNames = new HashSet<>();
        Map<String, Integer> emailCounts = new HashMap<>();
        
        for (GitCommit commit : commits) {
            String originalEmail = commit.authorEmail();
            String resolvedEmail = resolveEmailAlias(aliasConfig, originalEmail);
            
            // If this commit's email resolves to our canonical email, include it
            if (resolvedEmail.equals(canonicalEmail)) {
                allEmails.add(originalEmail);
                allNames.add(commit.authorName());
                emailCounts.merge(originalEmail, 1, Integer::sum);
            }
        }
        
        // Add alias emails from config
        if (aliasConfig != null && aliasConfig.containsKey(canonicalEmail)) {
            allEmails.addAll(aliasConfig.get(canonicalEmail));
        }
        
        // Add canonical email itself
        allEmails.add(canonicalEmail);
        
        // Find best canonical name and primary email
        String canonicalName = findCanonicalName(new ArrayList<>(allNames), emailCounts);
        String primaryEmail = canonicalEmail; // Use the canonical email as primary
        
        return ContributorIdentity.builder()
            .canonicalName(canonicalName)
            .primaryEmail(primaryEmail)
            .allEmails(allEmails.stream().sorted().toList())
            .allNames(allNames.stream().sorted().toList())
            .build();
    }
}