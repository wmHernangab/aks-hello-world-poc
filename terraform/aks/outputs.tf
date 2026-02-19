output "cluster_name" {
  description = "The name of the AKS cluster"
  value       = azurerm_kubernetes_cluster.aks.name
}

output "namespace" {
  description = "The Kubernetes namespace"
  value       = kubernetes_namespace.app.metadata[0].name
}

output "service_name" {
  description = "The name of the Kubernetes service"
  value       = kubernetes_service.app.metadata[0].name
}

output "service_external_endpoint" {
  description = "The external endpoint of the service once available"
  value       = try(kubernetes_service.app.status[0].load_balancer[0].ingress[0].ip, kubernetes_service.app.status[0].load_balancer[0].ingress[0].hostname, null)
}
