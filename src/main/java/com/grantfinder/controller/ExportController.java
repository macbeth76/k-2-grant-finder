package com.grantfinder.controller;

import com.grantfinder.dto.GrantSearchCriteria;
import com.grantfinder.model.Grant;
import com.grantfinder.service.GrantService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.math.BigDecimal;
import java.util.List;

@Controller("/api/export")
public class ExportController {

    private final GrantService grantService;

    public ExportController(GrantService grantService) {
        this.grantService = grantService;
    }

    @Get("/csv{?category,grantType,minAmount,maxAmount,keyword,activeOnly}")
    public HttpResponse<String> exportCsv(@Nullable @QueryValue String category,
                                           @Nullable @QueryValue String grantType,
                                           @Nullable @QueryValue BigDecimal minAmount,
                                           @Nullable @QueryValue BigDecimal maxAmount,
                                           @Nullable @QueryValue String keyword,
                                           @Nullable @QueryValue Boolean activeOnly) {

        List<Grant> grants;
        boolean hasFilters = (category != null && !category.isBlank())
                || (grantType != null && !grantType.isBlank())
                || minAmount != null
                || maxAmount != null
                || (keyword != null && !keyword.isBlank());

        if (hasFilters) {
            GrantSearchCriteria criteria = new GrantSearchCriteria();
            criteria.setCategory(category);
            criteria.setGrantType(grantType);
            criteria.setMinAmount(minAmount);
            criteria.setMaxAmount(maxAmount);
            criteria.setKeyword(keyword);
            criteria.setActiveOnly(activeOnly != null ? activeOnly : true);
            grants = grantService.search(criteria);
        } else if (activeOnly != null && activeOnly) {
            grants = grantService.findActive();
        } else {
            grants = grantService.findAll();
        }

        String csv = buildCsv(grants);

        return HttpResponse.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"grants-export.csv\"")
                .contentType(new MediaType("text/csv"));
    }

    private String buildCsv(List<Grant> grants) {
        StringBuilder sb = new StringBuilder();

        // Header row
        sb.append("ID,Name,Organization,Description,Min Amount,Max Amount,Deadline,Eligibility,Category,Grant Type,Website,Active,Source,Source URL,Last Crawled\n");

        // Data rows
        for (Grant grant : grants) {
            sb.append(escapeCsvField(grant.getId() != null ? grant.getId().toString() : "")).append(",");
            sb.append(escapeCsvField(grant.getName())).append(",");
            sb.append(escapeCsvField(grant.getOrganization())).append(",");
            sb.append(escapeCsvField(grant.getDescription())).append(",");
            sb.append(escapeCsvField(grant.getMinAmount() != null ? grant.getMinAmount().toPlainString() : "")).append(",");
            sb.append(escapeCsvField(grant.getMaxAmount() != null ? grant.getMaxAmount().toPlainString() : "")).append(",");
            sb.append(escapeCsvField(grant.getDeadline() != null ? grant.getDeadline().toString() : "")).append(",");
            sb.append(escapeCsvField(grant.getEligibility())).append(",");
            sb.append(escapeCsvField(grant.getCategory())).append(",");
            sb.append(escapeCsvField(grant.getGrantType())).append(",");
            sb.append(escapeCsvField(grant.getWebsite())).append(",");
            sb.append(grant.isActive()).append(",");
            sb.append(escapeCsvField(grant.getSource())).append(",");
            sb.append(escapeCsvField(grant.getSourceUrl())).append(",");
            sb.append(escapeCsvField(grant.getLastCrawled() != null ? grant.getLastCrawled().toString() : ""));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Escapes a CSV field value. If the value contains commas, quotes, or newlines,
     * it is wrapped in double quotes with internal quotes doubled.
     */
    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
