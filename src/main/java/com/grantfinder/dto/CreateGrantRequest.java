package com.grantfinder.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Serdeable
public class CreateGrantRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String organization;

    @NotBlank
    private String description;

    private BigDecimal minAmount;

    @NotNull
    private BigDecimal maxAmount;

    private LocalDate deadline;

    private String eligibility;

    @NotBlank
    private String category;

    @NotBlank
    private String grantType;

    private String website;

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
}
