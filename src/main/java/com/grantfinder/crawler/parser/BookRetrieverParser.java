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
public class BookRetrieverParser implements GrantSiteParser {

    private static final Logger LOG = LoggerFactory.getLogger(BookRetrieverParser.class);
    private static final String URL = "https://bookretriever.com/component/content/article?id=21:resources-for-building-your-classroom-library";
    private static final String USER_AGENT = "K2GrantFinder/0.1 (personal project)";

    @Override
    public CrawlSource getSource() {
        return CrawlSource.BOOKRETRIEVER;
    }

    @Override
    public List<Grant> parse() throws IOException {
        LOG.info("Crawling Book Retriever classroom library resources: {}", URL);
        List<Grant> grants = new ArrayList<>();

        Document doc = Jsoup.connect(URL)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        // BookRetriever uses headings (h2/h3/h4) with descriptions and links
        Elements headings = doc.select("article h2, article h3, article h4, .item-page h2, .item-page h3, .item-page h4");
        if (headings.isEmpty()) {
            headings = doc.select("h2, h3, h4");
        }

        LOG.info("Found {} headings on bookretriever.com", headings.size());

        for (Element heading : headings) {
            try {
                Grant grant = parseGrantBlock(heading);
                if (grant != null) {
                    grants.add(grant);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse heading '{}': {}", heading.text(), e.getMessage());
            }
        }

        // Fallback: parse all external links with grant-like text
        if (grants.isEmpty()) {
            grants = parseFallbackLinks(doc);
        }

        LOG.info("Parsed {} classroom library grants from Book Retriever", grants.size());
        return grants;
    }

    private Grant parseGrantBlock(Element heading) {
        String name = heading.text().trim();
        name = name.replaceFirst("^\\d+\\.?\\s*", "");
        if (name.isBlank() || name.length() < 4) return null;

        String lower = name.toLowerCase();
        if (lower.contains("comment") || lower.contains("navigation") || lower.contains("sidebar")
                || lower.contains("footer") || lower.contains("menu") || lower.contains("search")) {
            return null;
        }

        StringBuilder description = new StringBuilder();
        String amount = null;
        String link = null;
        String eligibility = null;

        Element headingLink = heading.selectFirst("a[href]");
        if (headingLink != null) {
            link = headingLink.absUrl("href");
        }

        Element sibling = heading.nextElementSibling();
        int maxSiblings = 10;
        while (sibling != null && maxSiblings-- > 0) {
            String tag = sibling.tagName();
            if (tag.matches("h[1-6]")) break;

            String text = sibling.text().trim();
            if (text.isEmpty()) {
                sibling = sibling.nextElementSibling();
                continue;
            }

            description.append(text).append(" ");

            String textLower = text.toLowerCase();
            if (textLower.contains("$") && amount == null) {
                amount = text;
            }
            if ((textLower.contains("school") || textLower.contains("teacher") || textLower.contains("librarian")
                    || textLower.contains("k-") || textLower.contains("elementary")) && eligibility == null) {
                eligibility = text;
            }

            if (link == null) {
                Element pLink = sibling.selectFirst("a[href]");
                if (pLink != null) {
                    String href = pLink.absUrl("href");
                    if (!href.contains("bookretriever.com")) {
                        link = href;
                    }
                }
            }

            sibling = sibling.nextElementSibling();
        }

        String descText = description.toString().trim();
        if (descText.isEmpty()) descText = name;

        Grant grant = new Grant();
        grant.setName(ParsingUtils.truncate(name, 255));
        grant.setOrganization(extractOrganization(name, descText));
        grant.setDescription(ParsingUtils.truncate(descText, 2000));
        grant.setCategory("CHILDREN");
        grant.setGrantType("PUBLISHING"); // Classroom library building
        grant.setActive(true);
        grant.setSource(CrawlSource.BOOKRETRIEVER.name());
        grant.setLastCrawled(LocalDate.now());

        if (link != null) {
            grant.setWebsite(link);
            grant.setSourceUrl(link);
        } else {
            grant.setSourceUrl(URL);
        }

        if (amount != null) {
            BigDecimal[] range = ParsingUtils.parseAmountRange(amount);
            grant.setMinAmount(range[0]);
            grant.setMaxAmount(range[1]);
        }

        if (eligibility != null) {
            grant.setEligibility(ParsingUtils.truncate(eligibility, 500));
        }

        return grant;
    }

    private List<Grant> parseFallbackLinks(Document doc) {
        List<Grant> grants = new ArrayList<>();
        Elements links = doc.select("article a[href], .item-page a[href]");

        for (Element link : links) {
            String text = link.text().trim();
            String href = link.absUrl("href");
            if (text.length() < 5 || text.length() > 200) continue;
            if (href.contains("bookretriever.com")) continue;
            if (href.isBlank()) continue;

            // Only include links that sound like grants/foundations
            String textLower = text.toLowerCase();
            boolean isRelevant = textLower.contains("foundation") || textLower.contains("grant")
                    || textLower.contains("award") || textLower.contains("fund")
                    || textLower.contains("library") || textLower.contains("book");
            if (!isRelevant) continue;

            Grant grant = new Grant();
            grant.setName(ParsingUtils.truncate(text, 255));
            grant.setOrganization(text);
            grant.setDescription("Classroom library funding resource: " + text);
            grant.setCategory("CHILDREN");
            grant.setGrantType("PUBLISHING");
            grant.setWebsite(href);
            grant.setSourceUrl(href);
            grant.setActive(true);
            grant.setSource(CrawlSource.BOOKRETRIEVER.name());
            grant.setLastCrawled(LocalDate.now());
            grants.add(grant);
        }

        return grants;
    }

    private String extractOrganization(String name, String desc) {
        String combined = (name + " " + desc).toLowerCase();
        if (combined.contains("pilcrow")) return "Pilcrow Foundation";
        if (combined.contains("snapdragon")) return "Snapdragon Book Foundation";
        if (combined.contains("national home library")) return "National Home Library Foundation";
        if (combined.contains("build-a-bear") || combined.contains("build a bear")) return "Build-A-Bear Foundation";
        if (combined.contains("laura bush")) return "Laura Bush Foundation";
        if (combined.contains("dollar general")) return "Dollar General Literacy Foundation";
        if (combined.contains("barnes") || combined.contains("noble")) return "Barnes & Noble";
        if (combined.contains("rgk")) return "RGK Foundation";
        if (combined.contains("kellogg")) return "W.K. Kellogg Foundation";
        return "Book Retriever Resources";
    }
}
