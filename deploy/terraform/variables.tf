variable "namespace" {
  description = "Kubernetes namespace for the k2-grant-finder application"
  type        = string
  default     = "k2-grant-finder"
}

variable "image_name" {
  description = "Docker image name for the k2-grant-finder application"
  type        = string
  default     = "k2-grant-finder"
}

variable "image_tag" {
  description = "Docker image tag"
  type        = string
  default     = "latest"
}

variable "replicas" {
  description = "Number of pod replicas for the deployment"
  type        = number
  default     = 1
}

variable "postgres_host" {
  description = "PostgreSQL host address (Docker container running on royaloak)"
  type        = string
  default     = "192.168.0.211"
}

variable "postgres_port" {
  description = "PostgreSQL port"
  type        = number
  default     = 5433
}

variable "postgres_db" {
  description = "PostgreSQL database name"
  type        = string
  default     = "grantfinder"
}

variable "postgres_username" {
  description = "PostgreSQL username"
  type        = string
  default     = "grantfinder"
}

variable "postgres_password" {
  description = "PostgreSQL password"
  type        = string
  default     = "grantfinder"
  sensitive   = true
}

variable "ntfy_url" {
  description = "ntfy notification server URL (running in K8s namespace ntfy on NodePort 30090)"
  type        = string
  default     = "http://192.168.0.211:30090"
}

variable "ingress_host" {
  description = "Hostname for the Traefik ingress"
  type        = string
  default     = "grants.royaloak02"
}

variable "app_port" {
  description = "Port the Micronaut application listens on"
  type        = number
  default     = 8080
}
