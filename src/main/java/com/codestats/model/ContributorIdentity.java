package com.codestats.model;

import java.util.List;

/**
 * Immutable record representing a unified contributor identity.
 * Handles the case where one person commits using multiple emails/names.
 */
public record ContributorIdentity(
    String canonicalName,
    String primaryEmail,
    List<String> allEmails,
    List<String> allNames
) {
    
    /**
     * Check if this identity matches a given email
     */
    public boolean hasEmail(String email) {
        return allEmails.contains(email);
    }
    
    /**
     * Check if this identity matches a given name
     */
    public boolean hasName(String name) {
        return allNames.contains(name);
    }
    
    /**
     * Get a display string showing all known aliases
     */
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(canonicalName);
        
        if (allEmails.size() > 1) {
            sb.append(" (also: ");
            allEmails.stream()
                .filter(email -> !email.equals(primaryEmail))
                .forEach(email -> sb.append(email).append(", "));
            
            // Remove trailing ", "
            if (sb.length() > canonicalName.length() + 7) {
                sb.setLength(sb.length() - 2);
            }
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Builder for functional construction
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String canonicalName;
        private String primaryEmail;
        private List<String> allEmails = List.of();
        private List<String> allNames = List.of();
        
        public Builder canonicalName(String canonicalName) {
            this.canonicalName = canonicalName;
            return this;
        }
        
        public Builder primaryEmail(String primaryEmail) {
            this.primaryEmail = primaryEmail;
            return this;
        }
        
        public Builder allEmails(List<String> allEmails) {
            this.allEmails = List.copyOf(allEmails);
            return this;
        }
        
        public Builder allNames(List<String> allNames) {
            this.allNames = List.copyOf(allNames);
            return this;
        }
        
        public ContributorIdentity build() {
            return new ContributorIdentity(canonicalName, primaryEmail, allEmails, allNames);
        }
    }
}