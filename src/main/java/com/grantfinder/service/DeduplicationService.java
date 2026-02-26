package com.grantfinder.service;

import com.grantfinder.model.Grant;
import com.grantfinder.repository.GrantRepository;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class DeduplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(DeduplicationService.class);

    /**
     * Similarity threshold (0.0 to 1.0). Grants with normalized name similarity
     * at or above this value are considered potential duplicates.
     */
    private static final double SIMILARITY_THRESHOLD = 0.85;

    private final GrantRepository grantRepository;

    public DeduplicationService(GrantRepository grantRepository) {
        this.grantRepository = grantRepository;
    }

    /**
     * Finds and merges duplicate grants. Keeps the grant with the most complete data
     * (most non-null fields) and marks others as inactive.
     *
     * @return the number of grants that were marked inactive as duplicates
     */
    public int deduplicate() {
        List<Grant> activeGrants = grantRepository.findByActiveTrue();
        LOG.info("Starting deduplication across {} active grants", activeGrants.size());

        Set<Long> alreadyMerged = new HashSet<>();
        int mergedCount = 0;

        for (int i = 0; i < activeGrants.size(); i++) {
            Grant grantA = activeGrants.get(i);
            if (alreadyMerged.contains(grantA.getId())) {
                continue;
            }

            List<Grant> duplicates = new ArrayList<>();

            for (int j = i + 1; j < activeGrants.size(); j++) {
                Grant grantB = activeGrants.get(j);
                if (alreadyMerged.contains(grantB.getId())) {
                    continue;
                }

                if (areDuplicates(grantA, grantB)) {
                    duplicates.add(grantB);
                }
            }

            if (!duplicates.isEmpty()) {
                // Add grantA to the group so we can pick the best one
                duplicates.add(0, grantA);

                // Find the grant with the most complete data
                Grant best = findMostComplete(duplicates);

                // Merge data from others into the best one
                for (Grant dup : duplicates) {
                    if (!dup.getId().equals(best.getId())) {
                        mergeInto(best, dup);
                        dup.setActive(false);
                        grantRepository.update(dup);
                        alreadyMerged.add(dup.getId());
                        mergedCount++;
                        LOG.debug("Marked grant id={} '{}' as duplicate of id={} '{}'",
                                dup.getId(), dup.getName(), best.getId(), best.getName());
                    }
                }

                // Update the best grant with any merged data
                grantRepository.update(best);
            }
        }

        LOG.info("Deduplication complete: {} grants merged as duplicates", mergedCount);
        return mergedCount;
    }

    /**
     * Determines whether two grants are likely duplicates based on fuzzy name matching
     * and optional organization matching.
     */
    boolean areDuplicates(Grant a, Grant b) {
        String nameA = normalize(a.getName());
        String nameB = normalize(b.getName());

        // Exact normalized name match is always a duplicate
        if (nameA.equals(nameB)) {
            return true;
        }

        // Fuzzy name match using Levenshtein distance
        double nameSimilarity = calculateSimilarity(nameA, nameB);
        if (nameSimilarity >= SIMILARITY_THRESHOLD) {
            // If names are very similar, check if organizations also match or are missing
            String orgA = normalize(a.getOrganization());
            String orgB = normalize(b.getOrganization());

            if (orgA.isEmpty() || orgB.isEmpty()) {
                // If either organization is missing, rely on name similarity alone
                return true;
            }

            double orgSimilarity = calculateSimilarity(orgA, orgB);
            // If orgs are somewhat similar (lower threshold since org names vary more)
            return orgSimilarity >= 0.7;
        }

        return false;
    }

    /**
     * Calculates the similarity between two strings using Levenshtein distance,
     * normalized to a 0.0-1.0 range.
     */
    double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        int distance = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Computes the Levenshtein edit distance between two strings.
     */
    int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        // Use two rows instead of full matrix for memory efficiency
        int[] prev = new int[len2 + 1];
        int[] curr = new int[len2 + 1];

        for (int j = 0; j <= len2; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            curr[0] = i;
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[len2];
    }

    /**
     * Finds the grant with the most non-null/non-empty fields.
     */
    private Grant findMostComplete(List<Grant> grants) {
        Grant best = grants.get(0);
        int bestScore = completenessScore(best);

        for (int i = 1; i < grants.size(); i++) {
            int score = completenessScore(grants.get(i));
            if (score > bestScore) {
                best = grants.get(i);
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * Scores a grant based on how many fields are filled in (non-null, non-empty).
     */
    private int completenessScore(Grant grant) {
        int score = 0;
        if (grant.getName() != null && !grant.getName().isBlank()) score++;
        if (grant.getOrganization() != null && !grant.getOrganization().isBlank()) score++;
        if (grant.getDescription() != null && !grant.getDescription().isBlank()) score++;
        if (grant.getMinAmount() != null) score++;
        if (grant.getMaxAmount() != null) score++;
        if (grant.getDeadline() != null) score++;
        if (grant.getEligibility() != null && !grant.getEligibility().isBlank()) score++;
        if (grant.getCategory() != null && !grant.getCategory().isBlank()) score++;
        if (grant.getGrantType() != null && !grant.getGrantType().isBlank()) score++;
        if (grant.getWebsite() != null && !grant.getWebsite().isBlank()) score++;
        if (grant.getSource() != null && !grant.getSource().isBlank()) score++;
        if (grant.getSourceUrl() != null && !grant.getSourceUrl().isBlank()) score++;
        if (grant.getLastCrawled() != null) score++;
        return score;
    }

    /**
     * Merges non-null fields from the source grant into the target grant
     * (only fills in fields that are missing in the target).
     */
    private void mergeInto(Grant target, Grant source) {
        if ((target.getDescription() == null || target.getDescription().isBlank())
                && source.getDescription() != null && !source.getDescription().isBlank()) {
            target.setDescription(source.getDescription());
        }
        if (target.getMinAmount() == null && source.getMinAmount() != null) {
            target.setMinAmount(source.getMinAmount());
        }
        if (target.getMaxAmount() == null && source.getMaxAmount() != null) {
            target.setMaxAmount(source.getMaxAmount());
        }
        if (target.getDeadline() == null && source.getDeadline() != null) {
            target.setDeadline(source.getDeadline());
        }
        if ((target.getEligibility() == null || target.getEligibility().isBlank())
                && source.getEligibility() != null && !source.getEligibility().isBlank()) {
            target.setEligibility(source.getEligibility());
        }
        if ((target.getCategory() == null || target.getCategory().isBlank())
                && source.getCategory() != null && !source.getCategory().isBlank()) {
            target.setCategory(source.getCategory());
        }
        if ((target.getGrantType() == null || target.getGrantType().isBlank())
                && source.getGrantType() != null && !source.getGrantType().isBlank()) {
            target.setGrantType(source.getGrantType());
        }
        if ((target.getWebsite() == null || target.getWebsite().isBlank())
                && source.getWebsite() != null && !source.getWebsite().isBlank()) {
            target.setWebsite(source.getWebsite());
        }
        if ((target.getSourceUrl() == null || target.getSourceUrl().isBlank())
                && source.getSourceUrl() != null && !source.getSourceUrl().isBlank()) {
            target.setSourceUrl(source.getSourceUrl());
        }
    }

    /**
     * Normalizes a string for comparison: lowercase, trimmed, extra whitespace collapsed.
     */
    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
