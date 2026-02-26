package com.grantfinder.crawler;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface CrawlRunRepository extends CrudRepository<CrawlRun, Long> {

    @Query("SELECT * FROM crawl_runs ORDER BY started_at DESC")
    List<CrawlRun> findAllOrderByStartedAtDesc();
}
