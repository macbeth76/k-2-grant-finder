package com.grantfinder.notification;

import com.grantfinder.model.Grant;
import com.grantfinder.repository.GrantRepository;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DeadlineChecker {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineChecker.class);

    private final GrantRepository grantRepository;
    private final NtfyService ntfyService;

    public DeadlineChecker(GrantRepository grantRepository, NtfyService ntfyService) {
        this.grantRepository = grantRepository;
        this.ntfyService = ntfyService;
    }

    /**
     * Runs daily at 8:00 AM to check for grants with deadlines in the next 7 days.
     */
    @Scheduled(cron = "0 0 8 * * *")
    void checkDeadlines() {
        LOG.info("Running daily deadline check");

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);

        List<Grant> allActiveGrants = grantRepository.findByActiveTrue();
        List<NtfyService.DeadlineInfo> approachingDeadlines = new ArrayList<>();

        for (Grant grant : allActiveGrants) {
            if (grant.getDeadline() != null) {
                LocalDate deadline = grant.getDeadline();
                if (!deadline.isBefore(today) && !deadline.isAfter(sevenDaysFromNow)) {
                    approachingDeadlines.add(new NtfyService.DeadlineInfo(
                            grant.getName(),
                            grant.getOrganization(),
                            grant.getDeadline()
                    ));
                }
            }
        }

        if (!approachingDeadlines.isEmpty()) {
            LOG.info("Found {} grants with approaching deadlines", approachingDeadlines.size());
            ntfyService.notifyDeadlinesBatch(approachingDeadlines);
        } else {
            LOG.info("No grants with approaching deadlines found");
        }
    }
}
