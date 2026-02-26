package com.grantfinder.controller;

import com.grantfinder.crawler.CrawlRun;
import com.grantfinder.crawler.CrawlSource;
import com.grantfinder.crawler.CrawlerService;
import com.grantfinder.dto.GrantSearchCriteria;
import com.grantfinder.model.Grant;
import com.grantfinder.service.GrantService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.views.View;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class WebController {

    private final GrantService grantService;
    private final CrawlerService crawlerService;

    public WebController(GrantService grantService, CrawlerService crawlerService) {
        this.grantService = grantService;
        this.crawlerService = crawlerService;
    }

    @View("index")
    @Get("/")
    public Map<String, Object> index(@Nullable @QueryValue String keyword,
                                     @Nullable @QueryValue String category,
                                     @Nullable @QueryValue String grantType) {
        Map<String, Object> model = new HashMap<>();

        GrantSearchCriteria criteria = new GrantSearchCriteria();
        boolean hasSearch = false;

        if (keyword != null && !keyword.isBlank()) {
            criteria.setKeyword(keyword);
            hasSearch = true;
        }
        if (category != null && !category.isBlank()) {
            criteria.setCategory(category);
            hasSearch = true;
        }
        if (grantType != null && !grantType.isBlank()) {
            criteria.setGrantType(grantType);
            hasSearch = true;
        }

        List<Grant> grants;
        if (hasSearch) {
            criteria.setActiveOnly(true);
            grants = grantService.search(criteria);
        } else {
            grants = grantService.findActive();
        }

        model.put("grants", grants);
        model.put("grantCount", grants.size());
        model.put("keyword", keyword != null ? keyword : "");
        model.put("selectedCategory", category != null ? category : "");
        model.put("selectedGrantType", grantType != null ? grantType : "");

        List<String> categories = Arrays.asList(
                "FICTION", "NON_FICTION", "POETRY", "CHILDREN", "RESEARCH",
                "STEM", "LITERACY", "ARTS", "TECHNOLOGY", "EDUCATION", "GENERAL"
        );
        List<String> grantTypes = Arrays.asList(
                "WRITING", "PUBLISHING", "RESEARCH", "RESIDENCY",
                "CLASSROOM", "PROFESSIONAL_DEVELOPMENT", "EQUIPMENT", "PROGRAM", "GENERAL"
        );

        model.put("categories", categories);
        model.put("grantTypes", grantTypes);

        return model;
    }

    @View("detail")
    @Get("/grants/{id}")
    public HttpResponse<?> detail(@PathVariable Long id) {
        Optional<Grant> grantOpt = grantService.findById(id);
        if (grantOpt.isEmpty()) {
            return HttpResponse.redirect(URI.create("/"));
        }

        Map<String, Object> model = new HashMap<>();
        model.put("grant", grantOpt.get());
        return HttpResponse.ok(model);
    }

    @View("crawler")
    @Get("/crawler")
    public Map<String, Object> crawler() {
        Map<String, Object> model = new HashMap<>();

        List<CrawlRun> runs = crawlerService.getRecentRuns();
        model.put("runs", runs);

        List<String> sources = Arrays.stream(CrawlSource.values())
                .filter(s -> s != CrawlSource.MANUAL)
                .map(CrawlSource::name)
                .collect(Collectors.toList());
        model.put("sources", sources);

        return model;
    }

    @Post("/crawler/run-all")
    public HttpResponse<?> triggerCrawlAll() {
        crawlerService.crawlAll();
        return HttpResponse.redirect(URI.create("/crawler"));
    }

    @Post("/crawler/run/{source}")
    public HttpResponse<?> triggerCrawlSource(@PathVariable String source) {
        try {
            CrawlSource crawlSource = CrawlSource.valueOf(source.toUpperCase());
            crawlerService.crawlSource(crawlSource);
        } catch (IllegalArgumentException e) {
            // ignore invalid source, redirect back
        }
        return HttpResponse.redirect(URI.create("/crawler"));
    }
}
