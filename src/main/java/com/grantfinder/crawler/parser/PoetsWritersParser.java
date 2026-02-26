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
public class PoetsWritersParser implements GrantSiteParser {

    private static final Logger LOG = LoggerFactory.getLogger(PoetsWritersParser.class);
    private static final String BASE_URL = "https://www.pw.org/grants";
    private static final String USER_AGENT = "K2GrantFinder/0.1 (personal project)";

    @Override
    public CrawlSource getSource() {
        return CrawlSource.POETS_WRITERS;
    }

    @Override
    public List<Grant> parse() throws IOException {
        LOG.info("Crawling Poets & Writers grants: {}", BASE_URL);
        List<Grant> grants = new ArrayList<>();

        Document doc = Jsoup.connect(BASE_URL)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        // pw.org uses view-content divs or listing rows for grants
        Elements listings = doc.select(".view-content .views-row, .view-content .node--type-grant, .view-content article");
        if (listings.isEmpty()) {
            // Fallback: try broader selectors
            listings = doc.select(".views-row, .view-content li, .field-content");
        }

        LOG.info("Found {} raw listings on pw.org", listings.size());

        for (Element listing : listings) {
            try {
                Grant grant = parseListingElement(listing);
                if (grant != null && grant.getName() != null && !grant.getName().isBlank()) {
                    grants.add(grant);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse a pw.org listing element: {}", e.getMessage());
            }
        }

        // If structured parsing found nothing, try link-based extraction
        if (grants.isEmpty()) {
            grants = parseFallbackLinks(doc);
        }

        LOG.info("Parsed {} grants from Poets & Writers", grants.size());
        return grants;
    }

    private Grant parseListingElement(Element el) {
        String name = extractText(el, "h2 a, h3 a, .views-field-title a, .field--name-title a, a");
        String link = extractHref(el, "h2 a, h3 a, .views-field-title a, .field--name-title a, a");
        String description = extractText(el, ".field--name-body, .views-field-body, .field-content p, p");
        String deadline = extractText(el, ".views-field-field-deadline, .field--name-field-deadline, .date-display-single");
        String amount = extractText(el, ".views-field-field-award-amount, .field--name-field-award-amount");

        if (name == null || name.isBlank()) return null;

        Grant grant = new Grant();
        grant.setName(ParsingUtils.truncate(name.trim(), 255));
        grant.setOrganization("Poets & Writers");
        grant.setDescription(ParsingUtils.truncate(description != null ? description.trim() : name, 2000));
        grant.setCategory(ParsingUtils.classifyCategory(name + " " + (description != null ? description : "")));
        grant.setGrantType(ParsingUtils.classifyGrantType(name + " " + (description != null ? description : "")));
        grant.setActive(true);
        grant.setSource(CrawlSource.POETS_WRITERS.name());
        grant.setLastCrawled(LocalDate.now());

        if (link != null && !link.isBlank()) {
            if (link.startsWith("/")) link = "https://www.pw.org" + link;
            grant.setWebsite(link);
            grant.setSourceUrl(link);
        }

        if (deadline != null) {
            grant.setDeadline(ParsingUtils.parseDate(deadline));
        }

        if (amount != null) {
            BigDecimal[] range = ParsingUtils.parseAmountRange(amount);
            grant.setMinAmount(range[0]);
            grant.setMaxAmount(range[1]);
        }

        return grant;
    }

    private List<Grant> parseFallbackLinks(Document doc) {
        List<Grant> grants = new ArrayList<>();
        Elements links = doc.select("a[href*=grant], a[href*=award], a[href*=fellowship]");

        for (Element link : links) {
            String name = link.text().trim();
            String href = link.absUrl("href");
            if (name.length() < 5 || name.length() > 200) continue;
            if (name.toLowerCase().contains("search") || name.toLowerCase().contains("login")) continue;

            Grant grant = new Grant();
            grant.setName(ParsingUtils.truncate(name, 255));
            grant.setOrganization("Poets & Writers");
            grant.setDescription(name);
            grant.setCategory(ParsingUtils.classifyCategory(name));
            grant.setGrantType(ParsingUtils.classifyGrantType(name));
            grant.setWebsite(href);
            grant.setSourceUrl(href);
            grant.setActive(true);
            grant.setSource(CrawlSource.POETS_WRITERS.name());
            grant.setLastCrawled(LocalDate.now());
            grants.add(grant);
        }

        return grants;
    }

    private String extractText(Element parent, String selectors) {
        for (String sel : selectors.split(",")) {
            Elements found = parent.select(sel.trim());
            if (!found.isEmpty()) {
                String text = found.first().text().trim();
                if (!text.isEmpty()) return text;
            }
        }
        return null;
    }

    private String extractHref(Element parent, String selectors) {
        for (String sel : selectors.split(",")) {
            Elements found = parent.select(sel.trim());
            if (!found.isEmpty()) {
                String href = found.first().attr("href");
                if (!href.isEmpty()) return href;
            }
        }
        return null;
    }
}
