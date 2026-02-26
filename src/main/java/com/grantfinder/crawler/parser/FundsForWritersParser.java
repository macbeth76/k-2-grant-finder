package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import com.grantfinder.model.Grant;
import jakarta.inject.Singleton;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FundsForWritersParser implements GrantSiteParser {

    private static final Logger LOG = LoggerFactory.getLogger(FundsForWritersParser.class);
    private static final String BASE_URL = "https://fundsforwriters.com/grants/";
    private static final String USER_AGENT = "K2GrantFinder/0.1 (personal project)";

    @Override
    public CrawlSource getSource() {
        return CrawlSource.FUNDSFORWRITERS;
    }

    @Override
    public List<Grant> parse() throws IOException {
        LOG.info("Crawling Funds for Writers: {}", BASE_URL);
        List<Grant> grants = new ArrayList<>();

        Document doc = Jsoup.connect(BASE_URL)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        // FundsForWriters uses blog post / article format with grant listings
        Elements articles = doc.select("article, .post, .entry-content, .type-post");
        if (articles.isEmpty()) {
            articles = doc.select(".entry-content");
        }

        LOG.info("Found {} article blocks on fundsforwriters.com", articles.size());

        // Strategy 1: Parse structured article listings
        for (Element article : articles) {
            try {
                List<Grant> parsed = parseArticle(article);
                grants.addAll(parsed);
            } catch (Exception e) {
                LOG.warn("Failed to parse fundsforwriters article: {}", e.getMessage());
            }
        }

        // Strategy 2: If we got nothing from articles, parse all grant-like links
        if (grants.isEmpty()) {
            grants = parseGrantLinks(doc);
        }

        LOG.info("Parsed {} grants from Funds for Writers", grants.size());
        return grants;
    }

    private List<Grant> parseArticle(Element article) {
        List<Grant> grants = new ArrayList<>();

        // Look for individual grant entries within the article
        // FundsForWriters typically lists grants as paragraphs or list items with bold names
        Elements entries = article.select("p, li");

        for (Element entry : entries) {
            String text = entry.text().trim();
            if (text.length() < 20) continue;

            // Look for grant-like content: contains dollar amounts or keywords
            boolean hasAmount = text.contains("$");
            boolean hasGrantKeyword = text.toLowerCase().matches(".*\\b(grant|fellowship|award|prize|scholarship|residency)\\b.*");

            if (!hasAmount && !hasGrantKeyword) continue;

            // Extract the link if present
            Element link = entry.selectFirst("a, strong a, b a");
            if (link == null) link = entry.selectFirst("a[href]");

            String name;
            String href = null;
            if (link != null) {
                name = link.text().trim();
                href = link.absUrl("href");
            } else {
                // Use first sentence or bold text as name
                Element bold = entry.selectFirst("strong, b");
                name = bold != null ? bold.text().trim() : text.substring(0, Math.min(100, text.length()));
            }

            if (name.length() < 5 || name.length() > 255) continue;

            Grant grant = new Grant();
            grant.setName(ParsingUtils.truncate(name, 255));
            grant.setOrganization("Funds for Writers");
            grant.setDescription(ParsingUtils.truncate(text, 2000));
            grant.setCategory(ParsingUtils.classifyCategory(text));
            grant.setGrantType(ParsingUtils.classifyGrantType(text));
            grant.setActive(true);
            grant.setSource(CrawlSource.FUNDSFORWRITERS.name());
            grant.setLastCrawled(LocalDate.now());

            if (href != null && !href.isBlank()) {
                grant.setWebsite(href);
                grant.setSourceUrl(href);
            } else {
                grant.setSourceUrl(BASE_URL);
            }

            BigDecimal[] amounts = ParsingUtils.parseAmountRange(text);
            grant.setMinAmount(amounts[0]);
            grant.setMaxAmount(amounts[1]);

            grants.add(grant);
        }

        return grants;
    }

    private List<Grant> parseGrantLinks(Document doc) {
        List<Grant> grants = new ArrayList<>();
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String text = link.text().trim();
            String href = link.absUrl("href");

            if (text.length() < 10 || text.length() > 200) continue;

            boolean isGrant = text.toLowerCase().matches(".*\\b(grant|fellowship|award|prize|fund)\\b.*");
            if (!isGrant) continue;
            if (href.contains("fundsforwriters.com/grants/") && href.equals(BASE_URL)) continue;

            Grant grant = new Grant();
            grant.setName(ParsingUtils.truncate(text, 255));
            grant.setOrganization("Funds for Writers");
            grant.setDescription(text);
            grant.setCategory(ParsingUtils.classifyCategory(text));
            grant.setGrantType(ParsingUtils.classifyGrantType(text));
            grant.setWebsite(href);
            grant.setSourceUrl(href);
            grant.setActive(true);
            grant.setSource(CrawlSource.FUNDSFORWRITERS.name());
            grant.setLastCrawled(LocalDate.now());
            grants.add(grant);
        }

        return grants;
    }
}
