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
public class StayingCoolLibraryParser implements GrantSiteParser {

    private static final Logger LOG = LoggerFactory.getLogger(StayingCoolLibraryParser.class);
    private static final String URL = "https://www.stayingcoolinthelibrary.us/10-library-grants-every-librarian-should-apply-for/";
    private static final String USER_AGENT = "K2GrantFinder/0.1 (personal project)";

    @Override
    public CrawlSource getSource() {
        return CrawlSource.STAYINGCOOL_LIBRARY;
    }

    @Override
    public List<Grant> parse() throws IOException {
        LOG.info("Crawling Staying Cool in the Library: {}", URL);
        List<Grant> grants = new ArrayList<>();

        Document doc = Jsoup.connect(URL)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        // This site uses h2 or h3 headings per grant in a WordPress blog post
        Elements headings = doc.select(".entry-content h2, .entry-content h3, article h2, article h3");
        if (headings.isEmpty()) {
            headings = doc.select("h2, h3");
        }

        LOG.info("Found {} headings on stayingcoolinthelibrary.us", headings.size());

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

        LOG.info("Parsed {} library grants from Staying Cool in the Library", grants.size());
        return grants;
    }

    private Grant parseGrantBlock(Element heading) {
        String name = heading.text().trim();
        // Strip numbering
        name = name.replaceFirst("^\\d+\\.?\\s*", "");
        if (name.isBlank() || name.length() < 5) return null;

        // Skip non-grant headings
        String lower = name.toLowerCase();
        if (lower.contains("comment") || lower.contains("related") || lower.contains("share")
                || lower.contains("about") || lower.contains("subscribe") || lower.contains("navigation")) {
            return null;
        }

        StringBuilder description = new StringBuilder();
        String amount = null;
        String deadline = null;
        String eligibility = null;
        String link = null;

        // Check heading for link
        Element headingLink = heading.selectFirst("a[href]");
        if (headingLink != null) {
            link = headingLink.absUrl("href");
        }

        // Walk siblings to collect grant details
        Element sibling = heading.nextElementSibling();
        int maxSiblings = 15;
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
            if ((textLower.contains("deadline") || textLower.contains("due")) && deadline == null) {
                deadline = text;
            }
            if ((textLower.contains("eligib") || textLower.contains("who can") || textLower.contains("school")
                    || textLower.contains("librarian")) && eligibility == null) {
                eligibility = text;
            }

            // Grab external links
            if (link == null) {
                Element pLink = sibling.selectFirst("a[href]");
                if (pLink != null) {
                    String href = pLink.absUrl("href");
                    if (!href.contains("stayingcoolinthelibrary")) {
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
        grant.setCategory("CHILDREN"); // All library grants are children/education focused
        grant.setGrantType(ParsingUtils.classifyGrantType(name + " " + descText));
        grant.setActive(true);
        grant.setSource(CrawlSource.STAYINGCOOL_LIBRARY.name());
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

        if (deadline != null) {
            grant.setDeadline(ParsingUtils.parseDate(deadline));
        }

        if (eligibility != null) {
            grant.setEligibility(ParsingUtils.truncate(eligibility, 500));
        }

        return grant;
    }

    private String extractOrganization(String name, String desc) {
        // Try to identify the org from common patterns in library grants
        String combined = (name + " " + desc).toLowerCase();
        if (combined.contains("dollar general")) return "Dollar General Literacy Foundation";
        if (combined.contains("laura bush")) return "Laura Bush Foundation";
        if (combined.contains("snapdragon")) return "Snapdragon Book Foundation";
        if (combined.contains("first book")) return "First Book";
        if (combined.contains("aasl")) return "American Association of School Librarians";
        if (combined.contains("ala ") || combined.contains("american library")) return "American Library Association";
        if (combined.contains("caplan")) return "Caplan Foundation";
        if (combined.contains("library of congress")) return "Library of Congress";
        if (combined.contains("eisner")) return "Will Eisner Foundation";
        return "Staying Cool Library Grants";
    }
}
