terraform {
  required_version = ">= 1.5.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.25"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12"
    }
  }
}

provider "kubernetes" {
  # Uses the default kubeconfig on royaloak (~/.kube/config)
  # K3s writes its kubeconfig to /etc/rancher/k3s/k3s.yaml by default.
  # If running Terraform on royaloak directly, either:
  #   export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
  # or copy it to ~/.kube/config.
  config_path    = "~/.kube/config"
  config_context = "default"
}

provider "helm" {
  kubernetes {
    config_path    = "~/.kube/config"
    config_context = "default"
  }
}
