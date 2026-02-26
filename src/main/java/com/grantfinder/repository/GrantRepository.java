package com.grantfinder.repository;

import com.grantfinder.model.Grant;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.util.List;

@JdbcRepository(dialect = Dialect.H2)
public interface GrantRepository extends CrudRepository<Grant, Long> {

    List<Grant> findByActiveTrue();

    List<Grant> findByCategory(String category);

    List<Grant> findByGrantType(String grantType);

    List<Grant> findByCategoryAndGrantType(String category, String grantType);

    List<Grant> findByMaxAmountGreaterThanEquals(BigDecimal amount);

    @Query("SELECT * FROM grants WHERE active = true AND " +
           "(LOWER(name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(organization) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Grant> searchByKeyword(String keyword);

    @Query("SELECT * FROM grants WHERE active = true " +
           "AND (:category IS NULL OR category = :category) " +
           "AND (:grantType IS NULL OR grant_type = :grantType) " +
           "AND (:minAmount IS NULL OR max_amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR min_amount <= :maxAmount)")
    List<Grant> searchGrants(@Nullable String category, @Nullable String grantType,
                             @Nullable BigDecimal minAmount, @Nullable BigDecimal maxAmount);

    java.util.Optional<Grant> findByNameAndOrganization(String name, String organization);

    List<Grant> findBySource(String source);
}
