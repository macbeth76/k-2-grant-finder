package com.grantfinder.notification;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Singleton
public class NtfyService {

    private static final Logger LOG = LoggerFactory.getLogger(NtfyService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final HttpClient httpClient;
    private final String ntfyUrl;
    private final String ntfyTopic;

    public NtfyService(@Client("${ntfy.url}") HttpClient httpClient,
                        @Value("${ntfy.url}") String ntfyUrl,
                        @Value("${ntfy.topic}") String ntfyTopic) {
        this.httpClient = httpClient;
        this.ntfyUrl = ntfyUrl;
        this.ntfyTopic = ntfyTopic;
    }

    /**
     * Send a notification when new grants are discovered during a crawl.
     */
    public void notifyNewGrantsDiscovered(int newGrantCount, int updatedGrantCount, String source) {
        if (newGrantCount == 0 && updatedGrantCount == 0) {
            return;
        }

        String title = "Grant Crawl Complete: " + source;
        StringBuilder message = new StringBuilder();
        if (newGrantCount > 0) {
            message.append(newGrantCount).append(" new grant").append(newGrantCount != 1 ? "s" : "").append(" discovered");
        }
        if (updatedGrantCount > 0) {
            if (message.length() > 0) {
                message.append(", ");
            }
            message.append(updatedGrantCount).append(" grant").append(updatedGrantCount != 1 ? "s" : "").append(" updated");
        }
        message.append(" from ").append(source).append(".");

        sendNotification(title, message.toString());
    }

    /**
     * Send a batch notification summarizing an entire crawl run across all sources.
     */
    public void notifyCrawlBatchComplete(int totalNew, int totalUpdated, int sourceCount) {
        if (totalNew == 0 && totalUpdated == 0) {
            return;
        }

        String title = "Full Crawl Complete";
        StringBuilder message = new StringBuilder();
        message.append("Crawled ").append(sourceCount).append(" source").append(sourceCount != 1 ? "s" : "").append(". ");
        if (totalNew > 0) {
            message.append(totalNew).append(" new grant").append(totalNew != 1 ? "s" : "");
        }
        if (totalUpdated > 0) {
            if (totalNew > 0) {
                message.append(", ");
            }
            message.append(totalUpdated).append(" updated");
        }
        message.append(".");

        sendNotification(title, message.toString());
    }

    /**
     * Send notifications for grants with approaching deadlines.
     */
    public void notifyDeadlineApproaching(String grantName, String organization, LocalDate deadline) {
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        String title = "Grant Deadline Approaching";
        String message = "\"" + grantName + "\" by " + (organization != null ? organization : "Unknown")
                + " has a deadline on " + deadline.format(DATE_FMT)
                + " (" + daysUntil + " day" + (daysUntil != 1 ? "s" : "") + " remaining).";

        sendNotification(title, message);
    }

    /**
     * Send a batch notification for multiple grants with approaching deadlines.
     */
    public void notifyDeadlinesBatch(List<DeadlineInfo> deadlines) {
        if (deadlines.isEmpty()) {
            return;
        }

        String title = deadlines.size() + " Grant Deadline" + (deadlines.size() != 1 ? "s" : "") + " Approaching";
        StringBuilder message = new StringBuilder();
        message.append("The following grants have deadlines within the next 7 days:\n\n");
        for (DeadlineInfo info : deadlines) {
            long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), info.deadline());
            message.append("- ").append(info.grantName());
            if (info.organization() != null && !info.organization().isBlank()) {
                message.append(" (").append(info.organization()).append(")");
            }
            message.append(" - ").append(info.deadline().format(DATE_FMT));
            message.append(" [").append(daysUntil).append(" day").append(daysUntil != 1 ? "s" : "").append("]\n");
        }

        sendNotification(title, message.toString());
    }

    private void sendNotification(String title, String messageBody) {
        try {
            String topicUrl = "/" + ntfyTopic;
            HttpRequest<String> request = HttpRequest.POST(topicUrl, messageBody)
                    .header("Title", title)
                    .header("Priority", "default")
                    .header("Tags", "books,grant");

            httpClient.toBlocking().exchange(request, String.class);
            LOG.info("Sent ntfy notification: {}", title);
        } catch (Exception e) {
            LOG.warn("Failed to send ntfy notification '{}': {}", title, e.getMessage());
        }
    }

    /**
     * Record type for deadline notification info.
     */
    public record DeadlineInfo(String grantName, String organization, LocalDate deadline) {
    }
}
