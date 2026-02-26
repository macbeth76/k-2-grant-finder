package com.grantfinder;

import com.grantfinder.model.Grant;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class GrantControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testListAllGrants() {
        List<Grant> grants = client.toBlocking().retrieve(
                HttpRequest.GET("/api/grants"),
                Argument.listOf(Grant.class)
        );
        assertNotNull(grants);
        assertFalse(grants.isEmpty());
    }

    @Test
    void testListActiveGrants() {
        List<Grant> grants = client.toBlocking().retrieve(
                HttpRequest.GET("/api/grants/active"),
                Argument.listOf(Grant.class)
        );
        assertNotNull(grants);
        assertTrue(grants.stream().allMatch(Grant::isActive));
    }

    @Test
    void testGetGrantById() {
        Grant grant = client.toBlocking().retrieve(
                HttpRequest.GET("/api/grants/1"),
                Grant.class
        );
        assertNotNull(grant);
        assertEquals(1L, grant.getId());
    }

    @Test
    void testSearchByCategory() {
        List<Grant> grants = client.toBlocking().retrieve(
                HttpRequest.GET("/api/grants/search?category=FICTION"),
                Argument.listOf(Grant.class)
        );
        assertNotNull(grants);
        assertTrue(grants.stream().allMatch(g -> "FICTION".equals(g.getCategory())));
    }

    @Test
    void testSearchByKeyword() {
        List<Grant> grants = client.toBlocking().retrieve(
                HttpRequest.GET("/api/grants/search?keyword=children"),
                Argument.listOf(Grant.class)
        );
        assertNotNull(grants);
        assertFalse(grants.isEmpty());
    }

    @Test
    void testSearchByGrantType() {
        List<Grant> grants = client.toBlocking().retrieve(
                HttpRequest.GET("/api/grants/search?grantType=RESIDENCY"),
                Argument.listOf(Grant.class)
        );
        assertNotNull(grants);
        assertTrue(grants.stream().allMatch(g -> "RESIDENCY".equals(g.getGrantType())));
    }
}
