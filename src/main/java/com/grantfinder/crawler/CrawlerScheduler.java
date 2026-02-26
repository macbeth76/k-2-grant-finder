package com.grantfinder.crawler;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Singleton
public class CrawlerScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlerScheduler.class);

    private final CrawlerService crawlerService;

    public CrawlerScheduler(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @Scheduled(cron = "0 0 6 * * MON")
    void weeklyCrawl() {
        LOG.info("Starting scheduled weekly crawl");
        List<CrawlResult> results = crawlerService.crawlAll();
        int totalFound = results.stream().mapToInt(CrawlResult::getGrantsFound).sum();
        int totalCreated = results.stream().mapToInt(CrawlResult::getGrantsCreated).sum();
        int totalUpdated = results.stream().mapToInt(CrawlResult::getGrantsUpdated).sum();
        LOG.info("Scheduled crawl complete: found={}, created={}, updated={}",
                totalFound, totalCreated, totalUpdated);
    }
}
