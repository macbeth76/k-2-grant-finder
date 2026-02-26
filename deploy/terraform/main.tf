# =============================================================================
# K2 Grant Finder - Kubernetes Deployment on K3s (royaloak02)
# =============================================================================
#
# NOTE: The application currently uses @JdbcRepository(dialect = Dialect.H2)
# in GrantRepository.java and CrawlRunRepository.java. Before deploying with
# PostgreSQL, update those annotations to:
#
#   @JdbcRepository(dialect = Dialect.POSTGRES)
#
# Also add the PostgreSQL JDBC driver to build.gradle:
#   runtimeOnly("org.postgresql:postgresql")
#
# And update the Flyway migration SQL to use PostgreSQL syntax:
#   - Replace BIGINT AUTO_INCREMENT with BIGSERIAL
#   - Or use GENERATED ALWAYS AS IDENTITY
#
# The Micronaut environment variables below will override application.yml
# datasource settings at runtime, so the application-prod.yml can be updated
# or left as-is (env vars take precedence).
# =============================================================================

# -----------------------------------------------------------------------------
# Namespace
# -----------------------------------------------------------------------------
resource "kubernetes_namespace" "k2_grant_finder" {
  metadata {
    name = var.namespace

    labels = {
      app        = "k2-grant-finder"
      managed-by = "terraform"
    }
  }
}

# -----------------------------------------------------------------------------
# ConfigMap - Application configuration
# -----------------------------------------------------------------------------
resource "kubernetes_config_map" "k2_grant_finder" {
  metadata {
    name      = "k2-grant-finder-config"
    namespace = kubernetes_namespace.k2_grant_finder.metadata[0].name

    labels = {
      app        = "k2-grant-finder"
      managed-by = "terraform"
    }
  }

  data = {
    # Micronaut datasource configuration (overrides application.yml)
    DATASOURCES_DEFAULT_URL               = "jdbc:postgresql://${var.postgres_host}:${var.postgres_port}/${var.postgres_db}"
    DATASOURCES_DEFAULT_DRIVER_CLASS_NAME  = "org.postgresql.Driver"
    DATASOURCES_DEFAULT_USERNAME           = var.postgres_username
    DATASOURCES_DEFAULT_DIALECT            = "POSTGRES"
    DATASOURCES_DEFAULT_SCHEMA__GENERATE   = "NONE"

    # Micronaut active environment (loads application-postgres.yml)
    MICRONAUT_ENVIRONMENTS = "postgres"

    # Micronaut server settings
    MICRONAUT_SERVER_PORT = tostring(var.app_port)

    # ntfy notification server for crawl alerts
    NTFY_URL   = var.ntfy_url
    NTFY_TOPIC = "k2-grant-finder"

    # Flyway configuration - use postgres-compatible migrations (not H2 AUTO_INCREMENT)
    FLYWAY_DATASOURCES_DEFAULT_ENABLED   = "true"
    FLYWAY_DATASOURCES_DEFAULT_LOCATIONS = "classpath:db/postgres-migration"
  }
}

# -----------------------------------------------------------------------------
# Secret - Database password
# -----------------------------------------------------------------------------
resource "kubernetes_secret" "k2_grant_finder_db" {
  metadata {
    name      = "k2-grant-finder-db-secret"
    namespace = kubernetes_namespace.k2_grant_finder.metadata[0].name

    labels = {
      app        = "k2-grant-finder"
      managed-by = "terraform"
    }
  }

  data = {
    DATASOURCES_DEFAULT_PASSWORD = var.postgres_password
  }

  type = "Opaque"
}

# -----------------------------------------------------------------------------
# Deployment
# -----------------------------------------------------------------------------
resource "kubernetes_deployment" "k2_grant_finder" {
  metadata {
    name      = "k2-grant-finder"
    namespace = kubernetes_namespace.k2_grant_finder.metadata[0].name

    labels = {
      app        = "k2-grant-finder"
      managed-by = "terraform"
    }
  }

  spec {
    replicas = var.replicas

    selector {
      match_labels = {
        app = "k2-grant-finder"
      }
    }

    template {
      metadata {
        labels = {
          app = "k2-grant-finder"
        }
      }

      spec {
        container {
          name  = "k2-grant-finder"
          image = "${var.image_name}:${var.image_tag}"

          # K3s with local images: never try to pull from a registry
          image_pull_policy = "Never"

          port {
            container_port = var.app_port
            protocol       = "TCP"
          }

          # All non-secret env vars from ConfigMap
          env_from {
            config_map_ref {
              name = kubernetes_config_map.k2_grant_finder.metadata[0].name
            }
          }

          # Database password from Secret
          env_from {
            secret_ref {
              name = kubernetes_secret.k2_grant_finder_db.metadata[0].name
            }
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "100m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }

          # Micronaut health endpoint
          liveness_probe {
            http_get {
              path = "/health"
              port = var.app_port
            }
            initial_delay_seconds = 30
            period_seconds        = 15
            failure_threshold     = 3
          }

          readiness_probe {
            http_get {
              path = "/health"
              port = var.app_port
            }
            initial_delay_seconds = 15
            period_seconds        = 10
            failure_threshold     = 3
          }
        }

        # Give the JVM time to shut down gracefully
        termination_grace_period_seconds = 30
      }
    }
  }

  depends_on = [
    kubernetes_config_map.k2_grant_finder,
    kubernetes_secret.k2_grant_finder_db,
  ]
}

# -----------------------------------------------------------------------------
# Service (ClusterIP)
# -----------------------------------------------------------------------------
resource "kubernetes_service" "k2_grant_finder" {
  metadata {
    name      = "k2-grant-finder"
    namespace = kubernetes_namespace.k2_grant_finder.metadata[0].name

    labels = {
      app        = "k2-grant-finder"
      managed-by = "terraform"
    }
  }

  spec {
    type = "ClusterIP"

    selector = {
      app = "k2-grant-finder"
    }

    port {
      name        = "http"
      port        = var.app_port
      target_port = var.app_port
      protocol    = "TCP"
    }
  }
}

# -----------------------------------------------------------------------------
# Ingress (Traefik) - routes grants.royaloak02 to the service
# -----------------------------------------------------------------------------
resource "kubernetes_ingress_v1" "k2_grant_finder" {
  metadata {
    name      = "k2-grant-finder"
    namespace = kubernetes_namespace.k2_grant_finder.metadata[0].name

    labels = {
      app        = "k2-grant-finder"
      managed-by = "terraform"
    }

    annotations = {
      # Traefik-specific annotations for K3s
      "kubernetes.io/ingress.class"                = "traefik"
      "traefik.ingress.kubernetes.io/router.entrypoints" = "web"
    }
  }

  spec {
    # Route for grants.royaloak02
    rule {
      host = var.ingress_host

      http {
        path {
          path      = "/"
          path_type = "Prefix"

          backend {
            service {
              name = kubernetes_service.k2_grant_finder.metadata[0].name

              port {
                number = var.app_port
              }
            }
          }
        }
      }
    }

    # Also accept grants.local as an alias
    rule {
      host = "grants.local"

      http {
        path {
          path      = "/"
          path_type = "Prefix"

          backend {
            service {
              name = kubernetes_service.k2_grant_finder.metadata[0].name

              port {
                number = var.app_port
              }
            }
          }
        }
      }
    }
  }
}

# -----------------------------------------------------------------------------
# CronJob - Weekly crawl trigger (Mondays at 6:00 AM, matching CrawlerScheduler)
# -----------------------------------------------------------------------------
# NOTE: The app already has @Scheduled(cron = "0 0 6 * * MON") in
# CrawlerScheduler.java, so this CronJob is a belt-and-suspenders approach.
# It ensures a crawl runs even if the pod was recently restarted and the
# in-process scheduler missed its window. The endpoint is idempotent.
# -----------------------------------------------------------------------------
resource "kubernetes_cron_job_v1" "weekly_crawl" {
  metadata {
    name      = "k2-grant-finder-weekly-crawl"
    namespace = kubernetes_namespace.k2_grant_finder.metadata[0].name

    labels = {
      app        = "k2-grant-finder"
      managed-by = "terraform"
    }
  }

  spec {
    # Every Monday at 6:15 AM (offset by 15 min from the in-app scheduler)
    schedule = "15 6 * * 1"

    # Keep last 3 successful and 1 failed job for debugging
    successful_jobs_history_limit = 3
    failed_jobs_history_limit     = 1

    # Do not start a new job if the previous one is still running
    concurrency_policy = "Forbid"

    job_template {
      metadata {
        labels = {
          app  = "k2-grant-finder"
          task = "weekly-crawl"
        }
      }

      spec {
        # Retry up to 2 times on failure
        backoff_limit = 2

        template {
          metadata {
            labels = {
              app  = "k2-grant-finder"
              task = "weekly-crawl"
            }
          }

          spec {
            container {
              name  = "crawl-trigger"
              image = "curlimages/curl:8.5.0"

              command = [
                "/bin/sh",
                "-c",
                "curl -sf -X POST http://k2-grant-finder.${var.namespace}.svc.cluster.local:${var.app_port}/api/crawler/run && echo 'Crawl triggered successfully'"
              ]
            }

            restart_policy = "OnFailure"
          }
        }
      }
    }
  }
}
