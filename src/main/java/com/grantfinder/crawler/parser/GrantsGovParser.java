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
public class GrantsGovParser implements GrantSiteParser {

    private static final Logger LOG = LoggerFactory.getLogger(GrantsGovParser.class);
    private static final String USER_AGENT = "K2GrantFinder/0.1 (personal project)";

    // Search grants.gov for arts/humanities book-related grants
    private static final String[] SEARCH_URLS = {
        "https://www.grants.gov/search-grants?cfda=45.024",   // NEA Promotion of the Arts: Literature
        "https://www.grants.gov/search-grants?cfda=45.164",   // NEH Promotion of the Humanities
    };

    @Override
    public CrawlSource getSource() {
        return CrawlSource.GRANTS_GOV;
    }

    @Override
    public List<Grant> parse() throws IOException {
        LOG.info("Crawling Grants.gov for book/arts grants");
        List<Grant> grants = new ArrayList<>();

        for (String url : SEARCH_URLS) {
            try {
                List<Grant> found = parsePage(url);
                grants.addAll(found);
                Thread.sleep(2000); // Be respectful between requests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOG.warn("Failed to crawl {}: {}", url, e.getMessage());
            }
        }

        LOG.info("Parsed {} grants from Grants.gov", grants.size());
        return grants;
    }

    private List<Grant> parsePage(String url) throws IOException {
        LOG.info("Fetching: {}", url);
        List<Grant> grants = new ArrayList<>();

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(20000)
                .get();

        // grants.gov uses table rows or result cards
        Elements rows = doc.select(".usa-table tbody tr, .grant-result, .views-row, table.usa-table tr");
        if (rows.isEmpty()) {
            rows = doc.select("[class*=result], [class*=grant], article");
        }

        LOG.info("Found {} listings on grants.gov page", rows.size());

        for (Element row : rows) {
            try {
                Grant grant = parseRow(row, url);
                if (grant != null) {
                    grants.add(grant);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse grants.gov row: {}", e.getMessage());
            }
        }

        return grants;
    }

    private Grant parseRow(Element row, String pageUrl) {
        // Try multiple selector strategies for grants.gov's structure
        String name = extractFirst(row, "td:nth-child(1) a, .grant-title a, a[href*=opportunity], h3 a, a");
        String link = extractHref(row, "td:nth-child(1) a, .grant-title a, a[href*=opportunity], h3 a, a");
        String agency = extractFirst(row, "td:nth-child(2), .agency-name, .grant-agency");
        String deadline = extractFirst(row, "td:nth-child(4), .close-date, .grant-deadline, [class*=date]");
        String amount = extractFirst(row, "td:nth-child(5), .award-ceiling, .grant-amount, [class*=amount]");

        if (name == null || name.isBlank() || name.length() < 5) return null;

        Grant grant = new Grant();
        grant.setName(ParsingUtils.truncate(name.trim(), 255));
        grant.setOrganization(agency != null ? ParsingUtils.truncate(agency.trim(), 255) : "Federal Government");
        grant.setDescription(name.trim());
        grant.setCategory(ParsingUtils.classifyCategory(name));
        grant.setGrantType(ParsingUtils.classifyGrantType(name));
        grant.setActive(true);
        grant.setSource(CrawlSource.GRANTS_GOV.name());
        grant.setLastCrawled(LocalDate.now());

        if (link != null) {
            if (link.startsWith("/")) link = "https://www.grants.gov" + link;
            grant.setWebsite(link);
            grant.setSourceUrl(link);
        } else {
            grant.setSourceUrl(pageUrl);
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

    private String extractFirst(Element parent, String selectors) {
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
