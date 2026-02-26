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
import java.util.Set;

@Singleton
public class WeAreTeachersParser implements GrantSiteParser {

    private static final Logger LOG = LoggerFactory.getLogger(WeAreTeachersParser.class);
    private static final String URL = "https://www.weareteachers.com/education-grants/";
    private static final String USER_AGENT = "K2GrantFinder/0.1 (personal project)";

    // Keywords to filter for literacy/book/reading grants
    private static final Set<String> LITERACY_KEYWORDS = Set.of(
            "book", "books", "reading", "literacy", "library", "libraries",
            "literature", "read", "writing", "classroom library"
    );

    @Override
    public CrawlSource getSource() {
        return CrawlSource.WEARETEACHERS;
    }

    @Override
    public List<Grant> parse() throws IOException {
        LOG.info("Crawling We Are Teachers: {}", URL);
        List<Grant> grants = new ArrayList<>();

        Document doc = Jsoup.connect(URL)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        // WeAreTeachers uses h3 headings for each grant with numbered list items
        // followed by description text. WordPress content structure.
        Elements headings = doc.select(".entry-content h3, .post-content h3, article h3");
        if (headings.isEmpty()) {
            headings = doc.select("h3");
        }

        LOG.info("Found {} h3 headings on weareteachers.com", headings.size());

        for (Element heading : headings) {
            try {
                Grant grant = parseGrantBlock(heading);
                if (grant != null) {
                    grants.add(grant);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse grant heading '{}': {}", heading.text(), e.getMessage());
            }
        }

        LOG.info("Parsed {} teacher grants from We Are Teachers", grants.size());
        return grants;
    }

    private Grant parseGrantBlock(Element heading) {
        String name = heading.text().trim();
        // Strip leading numbers like "1. " or "50. "
        name = name.replaceFirst("^\\d+\\.?\\s*", "");
        if (name.isBlank() || name.length() < 5) return null;

        // Collect sibling paragraphs until the next heading
        StringBuilder description = new StringBuilder();
        String amount = null;
        String deadline = null;
        String link = null;
        String eligibility = null;

        // Check for link in the heading itself
        Element headingLink = heading.selectFirst("a[href]");
        if (headingLink != null) {
            link = headingLink.absUrl("href");
        }

        Element sibling = heading.nextElementSibling();
        int maxSiblings = 10;
        while (sibling != null && maxSiblings-- > 0) {
            String tag = sibling.tagName();
            if (tag.matches("h[1-6]")) break; // Stop at next heading

            String text = sibling.text().trim();
            description.append(text).append(" ");

            // Look for structured fields
            String lower = text.toLowerCase();
            if (lower.contains("award") || lower.contains("amount") || lower.contains("grant size") || lower.contains("up to $")) {
                if (amount == null) amount = text;
            }
            if (lower.contains("deadline") || lower.contains("due date") || lower.contains("closes")) {
                if (deadline == null) deadline = text;
            }
            if (lower.contains("eligibility") || lower.contains("who can apply") || lower.contains("k-2") || lower.contains("k-12")) {
                if (eligibility == null) eligibility = text;
            }

            // Pick up links from paragraphs
            if (link == null) {
                Element pLink = sibling.selectFirst("a[href]");
                if (pLink != null) {
                    String href = pLink.absUrl("href");
                    if (!href.contains("weareteachers.com")) {
                        link = href;
                    }
                }
            }

            sibling = sibling.nextElementSibling();
        }

        String fullText = (name + " " + description).toLowerCase();

        // Filter: only keep grants relevant to books/reading/literacy for teachers
        boolean isRelevant = LITERACY_KEYWORDS.stream().anyMatch(fullText::contains)
                || fullText.contains("teacher") || fullText.contains("classroom")
                || fullText.contains("education") || fullText.contains("school");

        if (!isRelevant) return null;

        Grant grant = new Grant();
        grant.setName(ParsingUtils.truncate(name, 255));
        grant.setOrganization("We Are Teachers");
        grant.setDescription(ParsingUtils.truncate(description.toString().trim(), 2000));
        grant.setCategory(classifyTeacherCategory(fullText));
        grant.setGrantType("WRITING");
        grant.setActive(true);
        grant.setSource(CrawlSource.WEARETEACHERS.name());
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

    private String classifyTeacherCategory(String text) {
        if (text.contains("library") || text.contains("libraries")) return "CHILDREN";
        if (text.contains("poetry") || text.contains("poet")) return "POETRY";
        if (text.contains("research") || text.contains("academic")) return "RESEARCH";
        if (text.contains("nonfiction") || text.contains("non-fiction")) return "NON_FICTION";
        return "CHILDREN"; // Default for teacher grants
    }
}
