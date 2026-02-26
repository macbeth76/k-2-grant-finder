output "service_url" {
  description = "Internal cluster service URL for k2-grant-finder"
  value       = "http://${kubernetes_service.k2_grant_finder.metadata[0].name}.${kubernetes_namespace.k2_grant_finder.metadata[0].name}.svc.cluster.local:${var.app_port}"
}

output "ingress_host" {
  description = "External hostname for accessing k2-grant-finder via Traefik ingress"
  value       = var.ingress_host
}

output "ingress_url" {
  description = "Full URL to access the application through Traefik"
  value       = "http://${var.ingress_host}"
}

output "namespace" {
  description = "Kubernetes namespace where the app is deployed"
  value       = kubernetes_namespace.k2_grant_finder.metadata[0].name
}
