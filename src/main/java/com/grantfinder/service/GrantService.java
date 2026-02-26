package com.grantfinder.service;

import com.grantfinder.dto.CreateGrantRequest;
import com.grantfinder.dto.GrantSearchCriteria;
import com.grantfinder.model.Grant;
import com.grantfinder.repository.GrantRepository;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class GrantService {

    private final GrantRepository grantRepository;

    public GrantService(GrantRepository grantRepository) {
        this.grantRepository = grantRepository;
    }

    public List<Grant> findAll() {
        return (List<Grant>) grantRepository.findAll();
    }

    public List<Grant> findActive() {
        return grantRepository.findByActiveTrue();
    }

    public Optional<Grant> findById(Long id) {
        return grantRepository.findById(id);
    }

    public List<Grant> search(GrantSearchCriteria criteria) {
        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            return grantRepository.searchByKeyword(criteria.getKeyword());
        }

        boolean hasCategory = criteria.getCategory() != null && !criteria.getCategory().isBlank();
        boolean hasType = criteria.getGrantType() != null && !criteria.getGrantType().isBlank();

        if (hasCategory || hasType || criteria.getMinAmount() != null || criteria.getMaxAmount() != null) {
            return grantRepository.searchGrants(
                    hasCategory ? criteria.getCategory() : null,
                    hasType ? criteria.getGrantType() : null,
                    criteria.getMinAmount(),
                    criteria.getMaxAmount()
            );
        }

        if (criteria.getActiveOnly() != null && criteria.getActiveOnly()) {
            return grantRepository.findByActiveTrue();
        }

        return findAll();
    }

    public Grant create(CreateGrantRequest request) {
        Grant grant = new Grant();
        grant.setName(request.getName());
        grant.setOrganization(request.getOrganization());
        grant.setDescription(request.getDescription());
        grant.setMinAmount(request.getMinAmount());
        grant.setMaxAmount(request.getMaxAmount());
        grant.setDeadline(request.getDeadline());
        grant.setEligibility(request.getEligibility());
        grant.setCategory(request.getCategory());
        grant.setGrantType(request.getGrantType());
        grant.setWebsite(request.getWebsite());
        grant.setActive(true);
        return grantRepository.save(grant);
    }

    public Optional<Grant> update(Long id, CreateGrantRequest request) {
        return grantRepository.findById(id).map(existing -> {
            existing.setName(request.getName());
            existing.setOrganization(request.getOrganization());
            existing.setDescription(request.getDescription());
            existing.setMinAmount(request.getMinAmount());
            existing.setMaxAmount(request.getMaxAmount());
            existing.setDeadline(request.getDeadline());
            existing.setEligibility(request.getEligibility());
            existing.setCategory(request.getCategory());
            existing.setGrantType(request.getGrantType());
            existing.setWebsite(request.getWebsite());
            return grantRepository.update(existing);
        });
    }

    public boolean deactivate(Long id) {
        return grantRepository.findById(id).map(grant -> {
            grant.setActive(false);
            grantRepository.update(grant);
            return true;
        }).orElse(false);
    }

    public void delete(Long id) {
        grantRepository.deleteById(id);
    }
}
