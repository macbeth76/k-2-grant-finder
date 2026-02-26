package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import com.grantfinder.model.Grant;
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

/**
 * Reusable parser for WordPress-style grant listing pages.
 * These sites share a common pattern: h2/h3 headings per grant
 * with sibling paragraphs containing description, amounts, deadlines.
 */
public abstract class WordPressGrantPageParser implements GrantSiteParser {

    private static final Logger LOG = LoggerFactory.getLogger(WordPressGrantPageParser.class);
    private static final String USER_AGENT = "K2GrantFinder/0.1 (personal project)";

    protected abstract String getUrl();
    protected abstract String getSiteName();
    protected abstract String getDefaultCategory();

    @Override
    public List<Grant> parse() throws IOException {
        LOG.info("Crawling {}: {}", getSiteName(), getUrl());
        List<Grant> grants = new ArrayList<>();

        Document doc = Jsoup.connect(getUrl())
                .userAgent(USER_AGENT)
                .timeout(20000)
                .followRedirects(true)
                .get();

        // Try progressively broader selectors
        Elements headings = doc.select(".entry-content h2, .entry-content h3, .post-content h2, .post-content h3");
        if (headings.isEmpty()) {
            headings = doc.select("article h2, article h3, main h2, main h3");
        }
        if (headings.isEmpty()) {
            headings = doc.select("h2, h3");
        }

        LOG.info("Found {} headings on {}", headings.size(), getSiteName());

        for (Element heading : headings) {
            try {
                Grant grant = parseHeadingBlock(heading);
                if (grant != null) {
                    grants.add(grant);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse heading '{}' on {}: {}", heading.text(), getSiteName(), e.getMessage());
            }
        }

        // Fallback: extract grant-like links if headings yielded nothing
        if (grants.isEmpty()) {
            grants = parseFallbackLinks(doc);
        }

        LOG.info("Parsed {} grants from {}", grants.size(), getSiteName());
        return grants;
    }

    private Grant parseHeadingBlock(Element heading) {
        String name = heading.text().trim();
        // Strip leading numbers like "1. ", "12) ", "#3 "
        name = name.replaceFirst("^[#]?\\d+[.):]?\\s*", "");
        if (name.isBlank() || name.length() < 4 || name.length() > 300) return null;

        // Skip navigational/structural headings
        String lower = name.toLowerCase();
        if (isSkippable(lower)) return null;

        StringBuilder description = new StringBuilder();
        String amount = null;
        String deadline = null;
        String eligibility = null;
        String link = null;

        // Check heading itself for a link
        Element headingLink = heading.selectFirst("a[href]");
        if (headingLink != null) {
            String href = headingLink.absUrl("href");
            if (!href.isBlank() && !isSameSite(href)) {
                link = href;
            }
        }

        // Walk sibling elements to collect details
        Element sibling = heading.nextElementSibling();
        int maxSiblings = 12;
        while (sibling != null && maxSiblings-- > 0) {
            if (sibling.tagName().matches("h[1-6]")) break;

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
            if ((textLower.contains("deadline") || textLower.contains("due date") || textLower.contains("application period")) && deadline == null) {
                deadline = text;
            }
            if ((textLower.contains("eligib") || textLower.contains("who can apply") || textLower.contains("k-12")
                    || textLower.contains("k-2") || textLower.contains("teacher") || textLower.contains("educator")) && eligibility == null) {
                eligibility = text;
            }

            // Grab first external link from body paragraphs
            if (link == null) {
                Element pLink = sibling.selectFirst("a[href]");
                if (pLink != null) {
                    String href = pLink.absUrl("href");
                    if (!href.isBlank() && !isSameSite(href)) {
                        link = href;
                    }
                }
            }

            sibling = sibling.nextElementSibling();
        }

        String descText = description.toString().trim();
        if (descText.isEmpty()) descText = name;
        String fullText = (name + " " + descText).toLowerCase();

        Grant grant = new Grant();
        grant.setName(ParsingUtils.truncate(name, 255));
        grant.setOrganization(getSiteName());
        grant.setDescription(ParsingUtils.truncate(descText, 2000));
        grant.setCategory(classifyCategory(fullText));
        grant.setGrantType(ParsingUtils.classifyGrantType(fullText));
        grant.setActive(true);
        grant.setSource(getSource().name());
        grant.setLastCrawled(LocalDate.now());

        if (link != null) {
            grant.setWebsite(link);
            grant.setSourceUrl(link);
        } else {
            grant.setSourceUrl(getUrl());
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

    private List<Grant> parseFallbackLinks(Document doc) {
        List<Grant> grants = new ArrayList<>();
        Elements links = doc.select("article a[href], .entry-content a[href], .post-content a[href], main a[href]");

        for (Element link : links) {
            String text = link.text().trim();
            String href = link.absUrl("href");
            if (text.length() < 5 || text.length() > 200) continue;
            if (href.isBlank() || isSameSite(href)) continue;

            String textLower = text.toLowerCase();
            boolean isGrant = textLower.contains("grant") || textLower.contains("foundation")
                    || textLower.contains("fellowship") || textLower.contains("award")
                    || textLower.contains("fund") || textLower.contains("scholarship");
            if (!isGrant) continue;

            Grant grant = new Grant();
            grant.setName(ParsingUtils.truncate(text, 255));
            grant.setOrganization(getSiteName());
            grant.setDescription(text);
            grant.setCategory(classifyCategory(textLower));
            grant.setGrantType(ParsingUtils.classifyGrantType(textLower));
            grant.setWebsite(href);
            grant.setSourceUrl(href);
            grant.setActive(true);
            grant.setSource(getSource().name());
            grant.setLastCrawled(LocalDate.now());
            grants.add(grant);
        }

        return grants;
    }

    private String classifyCategory(String text) {
        if (text.contains("stem") || text.contains("science") || text.contains("math") || text.contains("technology")) return "RESEARCH";
        if (text.contains("music") || text.contains("art ") || text.contains("arts")) return "POETRY"; // reuse for arts
        if (text.contains("library") || text.contains("reading") || text.contains("literacy") || text.contains("book")) return "CHILDREN";
        if (text.contains("special ed") || text.contains("disability") || text.contains("inclusive")) return "NON_FICTION";
        return getDefaultCategory();
    }

    private boolean isSameSite(String href) {
        try {
            String host = new java.net.URL(getUrl()).getHost().replace("www.", "");
            return href.contains(host);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSkippable(String lower) {
        return lower.contains("comment") || lower.contains("related post") || lower.contains("share this")
                || lower.contains("about the author") || lower.contains("subscribe") || lower.contains("navigation")
                || lower.contains("table of contents") || lower.contains("sidebar") || lower.contains("footer")
                || lower.contains("leave a reply") || lower.contains("you may also") || lower.contains("recent post")
                || lower.startsWith("menu") || lower.startsWith("search") || lower.contains("cookie")
                || lower.contains("privacy policy") || lower.contains("sign up") || lower.contains("newsletter");
    }
}
