package com.grantfinder.crawler;

import com.grantfinder.crawler.parser.GrantSiteParser;
import com.grantfinder.model.Grant;
import com.grantfinder.notification.NtfyService;
import com.grantfinder.repository.GrantRepository;
import com.grantfinder.service.DeduplicationService;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class CrawlerService {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlerService.class);

    private final List<GrantSiteParser> parsers;
    private final GrantRepository grantRepository;
    private final CrawlRunRepository crawlRunRepository;
    private final NtfyService ntfyService;
    private final DeduplicationService deduplicationService;

    public CrawlerService(List<GrantSiteParser> parsers,
                          GrantRepository grantRepository,
                          CrawlRunRepository crawlRunRepository,
                          NtfyService ntfyService,
                          DeduplicationService deduplicationService) {
        this.parsers = parsers;
        this.grantRepository = grantRepository;
        this.crawlRunRepository = crawlRunRepository;
        this.ntfyService = ntfyService;
        this.deduplicationService = deduplicationService;
    }

    public List<CrawlResult> crawlAll() {
        LOG.info("Starting crawl of all {} sources", parsers.size());
        List<CrawlResult> results = new ArrayList<>();
        for (GrantSiteParser parser : parsers) {
            results.add(crawlSource(parser));
        }

        // Send batch notification for the full crawl
        int totalNew = results.stream().mapToInt(CrawlResult::getGrantsCreated).sum();
        int totalUpdated = results.stream().mapToInt(CrawlResult::getGrantsUpdated).sum();
        ntfyService.notifyCrawlBatchComplete(totalNew, totalUpdated, results.size());

        // Run deduplication after full crawl
        try {
            int deduped = deduplicationService.deduplicate();
            if (deduped > 0) {
                LOG.info("Post-crawl deduplication merged {} duplicate grants", deduped);
            }
        } catch (Exception e) {
            LOG.warn("Post-crawl deduplication failed: {}", e.getMessage());
        }

        LOG.info("Completed crawl of all sources. Total results: {}", results.size());
        return results;
    }

    public CrawlResult crawlSource(CrawlSource source) {
        return parsers.stream()
                .filter(p -> p.getSource() == source)
                .findFirst()
                .map(this::crawlSource)
                .orElseGet(() -> {
                    CrawlResult r = new CrawlResult();
                    r.setSource(source.name());
                    r.setErrors(1);
                    r.getErrorMessages().add("No parser found for source: " + source);
                    return r;
                });
    }

    private CrawlResult crawlSource(GrantSiteParser parser) {
        CrawlResult result = new CrawlResult();
        result.setSource(parser.getSource().name());
        result.setStartedAt(Instant.now());

        CrawlRun run = new CrawlRun();
        run.setSource(parser.getSource().name());
        run.setStartedAt(Instant.now());

        try {
            List<Grant> discovered = parser.parse();
            result.setGrantsFound(discovered.size());
            run.setGrantsFound(discovered.size());

            int created = 0;
            int updated = 0;
            for (Grant grant : discovered) {
                try {
                    Optional<Grant> existing = grantRepository.findByNameAndOrganization(
                            grant.getName(), grant.getOrganization());

                    if (existing.isPresent()) {
                        Grant ex = existing.get();
                        ex.setDescription(grant.getDescription());
                        ex.setDeadline(grant.getDeadline());
                        ex.setMinAmount(grant.getMinAmount());
                        ex.setMaxAmount(grant.getMaxAmount());
                        ex.setWebsite(grant.getWebsite());
                        ex.setSourceUrl(grant.getSourceUrl());
                        ex.setLastCrawled(LocalDate.now());
                        ex.setActive(true);
                        grantRepository.update(ex);
                        updated++;
                    } else {
                        grant.setLastCrawled(LocalDate.now());
                        grantRepository.save(grant);
                        created++;
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to save grant '{}': {}", grant.getName(), e.getMessage());
                    result.setErrors(result.getErrors() + 1);
                    result.getErrorMessages().add("Save failed: " + grant.getName() + " - " + e.getMessage());
                }
            }

            result.setGrantsCreated(created);
            result.setGrantsUpdated(updated);
            run.setGrantsCreated(created);
            run.setGrantsUpdated(updated);

            LOG.info("Crawl of {} complete: found={}, created={}, updated={}",
                    parser.getSource(), discovered.size(), created, updated);

            // Notify about new/updated grants from this source
            ntfyService.notifyNewGrantsDiscovered(created, updated, parser.getSource().name());

        } catch (Exception e) {
            LOG.error("Crawl of {} failed: {}", parser.getSource(), e.getMessage(), e);
            result.setErrors(result.getErrors() + 1);
            result.getErrorMessages().add("Crawl failed: " + e.getMessage());
            run.setErrors(run.getErrors() + 1);
            run.setErrorMessage(e.getMessage());
        }

        result.setCompletedAt(Instant.now());
        run.setCompletedAt(Instant.now());
        run.setErrors(result.getErrors());

        try {
            crawlRunRepository.save(run);
        } catch (Exception e) {
            LOG.warn("Failed to save crawl run record: {}", e.getMessage());
        }

        return result;
    }

    public List<CrawlRun> getRecentRuns() {
        return crawlRunRepository.findAllOrderByStartedAtDesc();
    }
}
