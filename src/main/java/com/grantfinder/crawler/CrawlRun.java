package com.grantfinder.crawler;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
@MappedEntity("crawl_runs")
public class CrawlRun {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    private String source;
    private Instant startedAt;
    private Instant completedAt;
    private int grantsFound;
    private int grantsCreated;
    private int grantsUpdated;
    private int errors;
    private String errorMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
