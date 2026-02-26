package com.grantfinder.crawler.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParsingUtils {

    private static final Pattern DOLLAR_AMOUNT = Pattern.compile("\\$([\\d,]+(?:\\.\\d{2})?)");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("MMMM d, yyyy"),
            DateTimeFormatter.ofPattern("MMM d, yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy")
    );

    private ParsingUtils() {}

    public static BigDecimal parseAmount(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher m = DOLLAR_AMOUNT.matcher(text);
        if (m.find()) {
            String cleaned = m.group(1).replace(",", "");
            return new BigDecimal(cleaned);
        }
        return null;
    }

    public static BigDecimal[] parseAmountRange(String text) {
        if (text == null || text.isBlank()) return new BigDecimal[]{null, null};
        Matcher m = DOLLAR_AMOUNT.matcher(text);
        BigDecimal first = null;
        BigDecimal second = null;
        if (m.find()) {
            first = new BigDecimal(m.group(1).replace(",", ""));
        }
        if (m.find()) {
            second = new BigDecimal(m.group(1).replace(",", ""));
        }
        if (second != null) {
            return new BigDecimal[]{first, second};
        }
        return new BigDecimal[]{first, first};
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        String cleaned = text.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    public static String classifyCategory(String text) {
        if (text == null) return "FICTION";
        String lower = text.toLowerCase();
        if (lower.contains("poetry") || lower.contains("poet")) return "POETRY";
        if (lower.contains("children") || lower.contains("young adult") || lower.contains("ya ") || lower.contains("picture book")) return "CHILDREN";
        if (lower.contains("nonfiction") || lower.contains("non-fiction") || lower.contains("essay") || lower.contains("memoir")) return "NON_FICTION";
        if (lower.contains("research") || lower.contains("academic") || lower.contains("scholarly")) return "RESEARCH";
        return "FICTION";
    }

    public static String classifyGrantType(String text) {
        if (text == null) return "WRITING";
        String lower = text.toLowerCase();
        if (lower.contains("residency") || lower.contains("retreat") || lower.contains("colony")) return "RESIDENCY";
        if (lower.contains("publish") || lower.contains("press") || lower.contains("print")) return "PUBLISHING";
        if (lower.contains("research") || lower.contains("study") || lower.contains("academic")) return "RESEARCH";
        return "WRITING";
    }

    public static String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen - 3) + "..." : text;
    }
}
