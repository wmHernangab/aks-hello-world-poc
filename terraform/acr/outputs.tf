output "acr_name" {
  description = "The name of the Container Registry"
  value       = azurerm_container_registry.hello_world.name
}

output "acr_login_server_address" {
  description = "The login server address of the Container Registry"
  value       = azurerm_container_registry.hello_world.login_server
}
