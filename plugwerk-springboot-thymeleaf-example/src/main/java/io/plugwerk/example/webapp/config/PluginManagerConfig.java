/*
 * Copyright (c) 2025-present devtank42 GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.plugwerk.example.webapp.config;

import io.plugwerk.spi.PlugwerkConfig;
import io.plugwerk.spi.PlugwerkPlugin;
import io.plugwerk.spi.extension.PlugwerkMarketplace;
import java.nio.file.Path;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the PF4J {@link PluginManager} and Plugwerk SDK as Spring beans.
 *
 * <p>The {@code plugwerk-client-plugin} ZIP must be present in the plugins directory. If absent,
 * the application still starts but marketplace operations (catalog, install, update) will be
 * unavailable.
 */
@Configuration
public class PluginManagerConfig {

  private static final Logger log = LoggerFactory.getLogger(PluginManagerConfig.class);

  @Value("${plugwerk.plugins-dir:./plugins}")
  private String pluginsDir;

  @Value("${plugwerk.server-url:http://localhost:8080}")
  private String serverUrl;

  @Value("${plugwerk.namespace:default}")
  private String namespace;

  @Value("${plugwerk.api-key:}")
  private String apiKey;

  @Bean(destroyMethod = "stopPlugins")
  public PluginManager pluginManager() {
    Path pluginsPath = Path.of(pluginsDir).toAbsolutePath();
    log.info("Starting PF4J plugin manager with plugins directory: {}", pluginsPath);

    DefaultPluginManager manager = new DefaultPluginManager(pluginsPath);
    manager.loadPlugins();
    manager.startPlugins();

    log.info(
        "Loaded plugins: {}",
        manager.getPlugins().stream()
            .map(p -> p.getPluginId() + "@" + p.getDescriptor().getVersion())
            .toList());

    // Configure the Plugwerk SDK plugin with server connection details.
    PluginWrapper wrapper = manager.getPlugin(PlugwerkPlugin.PLUGIN_ID);
    if (wrapper != null) {
      PlugwerkConfig.Builder configBuilder =
          new PlugwerkConfig.Builder(serverUrl, namespace).pluginDirectory(pluginsPath);
      if (apiKey != null && !apiKey.isBlank()) {
        configBuilder.apiKey(apiKey);
      }
      ((PlugwerkPlugin) wrapper.getPlugin()).configure(configBuilder.build());
      log.info("Plugwerk SDK plugin configured for {} (namespace: {})", serverUrl, namespace);
    } else {
      log.warn(
          "Plugwerk SDK plugin not found in {}. "
              + "Marketplace operations will be unavailable. "
              + "Copy plugwerk-client-plugin-<version>.zip into the plugins directory.",
          pluginsPath);
    }

    return manager;
  }

  @Bean
  public PlugwerkMarketplace plugwerkMarketplace(PluginManager pluginManager) {
    PluginWrapper wrapper = pluginManager.getPlugin(PlugwerkPlugin.PLUGIN_ID);
    if (wrapper == null) {
      return null;
    }
    return ((PlugwerkPlugin) wrapper.getPlugin()).marketplace();
  }
}
