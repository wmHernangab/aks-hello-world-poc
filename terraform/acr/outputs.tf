output "acr_name" {
  description = "The name of the Container Registry"
  value       = azurerm_container_registry.acr.name
}

output "acr_login_server_address" {
  description = "The login server address of the Container Registry"
  value       = azurerm_container_registry.acr.login_server
}
