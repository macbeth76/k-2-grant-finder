package com.grantfinder.controller;

import com.grantfinder.dto.CreateGrantRequest;
import com.grantfinder.dto.GrantSearchCriteria;
import com.grantfinder.model.Grant;
import com.grantfinder.service.GrantService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.validation.Validated;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

@Controller("/api/grants")
@Validated
public class GrantController {

    private final GrantService grantService;

    public GrantController(GrantService grantService) {
        this.grantService = grantService;
    }

    @Get
    public List<Grant> listAll() {
        return grantService.findAll();
    }

    @Get("/active")
    public List<Grant> listActive() {
        return grantService.findActive();
    }

    @Get("/{id}")
    public HttpResponse<Grant> getById(@PathVariable Long id) {
        return grantService.findById(id)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Get("/search{?category,grantType,minAmount,maxAmount,keyword,activeOnly}")
    public List<Grant> search(@Nullable @QueryValue String category,
                              @Nullable @QueryValue String grantType,
                              @Nullable @QueryValue BigDecimal minAmount,
                              @Nullable @QueryValue BigDecimal maxAmount,
                              @Nullable @QueryValue String keyword,
                              @Nullable @QueryValue Boolean activeOnly) {
        GrantSearchCriteria criteria = new GrantSearchCriteria();
        criteria.setCategory(category);
        criteria.setGrantType(grantType);
        criteria.setMinAmount(minAmount);
        criteria.setMaxAmount(maxAmount);
        criteria.setKeyword(keyword);
        criteria.setActiveOnly(activeOnly != null ? activeOnly : true);
        return grantService.search(criteria);
    }

    @Post
    public HttpResponse<Grant> create(@Body @Valid CreateGrantRequest request) {
        Grant created = grantService.create(request);
        return HttpResponse.created(created);
    }

    @Put("/{id}")
    public HttpResponse<Grant> update(@PathVariable Long id, @Body @Valid CreateGrantRequest request) {
        return grantService.update(id, request)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Patch("/{id}/deactivate")
    public HttpResponse<Void> deactivate(@PathVariable Long id) {
        if (grantService.deactivate(id)) {
            return HttpResponse.ok();
        }
        return HttpResponse.notFound();
    }

    @Delete("/{id}")
    public HttpResponse<Void> delete(@PathVariable Long id) {
        grantService.delete(id);
        return HttpResponse.noContent();
    }
}
