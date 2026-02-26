package com.grantfinder.crawler;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;

import java.util.List;

@Controller("/api/crawler")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @Post("/run")
    public List<CrawlResult> crawlAll() {
        return crawlerService.crawlAll();
    }

    @Post("/run/{source}")
    public HttpResponse<CrawlResult> crawlSource(@PathVariable String source) {
        try {
            CrawlSource crawlSource = CrawlSource.valueOf(source.toUpperCase());
            CrawlResult result = crawlerService.crawlSource(crawlSource);
            return HttpResponse.ok(result);
        } catch (IllegalArgumentException e) {
            CrawlResult err = new CrawlResult();
            err.setSource(source);
            err.setErrors(1);
            err.getErrorMessages().add("Unknown source: " + source + ". Valid: POETS_WRITERS, GRANTS_GOV, FUNDSFORWRITERS");
            return HttpResponse.badRequest(err);
        }
    }

    @Get("/history")
    public List<CrawlRun> history() {
        return crawlerService.getRecentRuns();
    }
}
