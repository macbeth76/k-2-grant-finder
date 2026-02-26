package com.grantfinder.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Serdeable
@MappedEntity("grants")
public class Grant {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    private String name;
    private String organization;
    private String description;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalDate deadline;
    private String eligibility;
    private String category;       // e.g. FICTION, NON_FICTION, POETRY, CHILDREN, RESEARCH
    private String grantType;      // e.g. WRITING, PUBLISHING, RESEARCH, RESIDENCY
    private String website;
    private boolean active;
    private String source;          // MANUAL, POETS_WRITERS, GRANTS_GOV, FUNDSFORWRITERS
    private String sourceUrl;
    private LocalDate lastCrawled;

    public Grant() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public String getEligibility() { return eligibility; }
    public void setEligibility(String eligibility) { this.eligibility = eligibility; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getGrantType() { return grantType; }
    public void setGrantType(String grantType) { this.grantType = grantType; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public LocalDate getLastCrawled() { return lastCrawled; }
    public void setLastCrawled(LocalDate lastCrawled) { this.lastCrawled = lastCrawled; }
}
