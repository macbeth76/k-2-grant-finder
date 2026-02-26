package com.grantfinder.crawler;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Serdeable
public class CrawlResult {

    private String source;
    private Instant startedAt;
    private Instant completedAt;
    private int grantsFound;
    private int grantsCreated;
    private int grantsUpdated;
    private int errors;
    private List<String> errorMessages = new ArrayList<>();

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public int getGrantsFound() { return grantsFound; }
    public void setGrantsFound(int grantsFound) { this.grantsFound = grantsFound; }

    public int getGrantsCreated() { return grantsCreated; }
    public void setGrantsCreated(int grantsCreated) { this.grantsCreated = grantsCreated; }

    public int getGrantsUpdated() { return grantsUpdated; }
    public void setGrantsUpdated(int grantsUpdated) { this.grantsUpdated = grantsUpdated; }

    public int getErrors() { return errors; }
    public void setErrors(int errors) { this.errors = errors; }

    public List<String> getErrorMessages() { return errorMessages; }
    public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
}
