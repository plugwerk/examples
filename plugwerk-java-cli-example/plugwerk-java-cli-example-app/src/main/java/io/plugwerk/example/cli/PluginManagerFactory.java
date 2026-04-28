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
package io.plugwerk.example.cli;

import io.plugwerk.spi.PlugwerkConfig;
import io.plugwerk.spi.PlugwerkPlugin;
import io.plugwerk.spi.extension.PlugwerkMarketplace;
import java.nio.file.Path;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the PF4J {@link PluginManager} and opens {@link PlugwerkMarketplace} connections used by
 * the CLI host.
 *
 * <p>The {@code plugwerk-client-plugin} ZIP must be present in the plugins directory before calling
 * {@link #create(Path)}. The SDK plugin is loaded and started automatically; a marketplace
 * connection is then opened on demand via {@link #getMarketplace(PluginManager, PlugwerkConfig)}.
 *
 * <p>The returned {@link PlugwerkMarketplace} is owned by the caller and must be closed (it
 * implements {@link AutoCloseable}). The CLI host registers a JVM shutdown hook to do so on exit.
 */
public class PluginManagerFactory {

  private static final Logger log = LoggerFactory.getLogger(PluginManagerFactory.class);
  private static final String PLUGIN_ID = PlugwerkPlugin.PLUGIN_ID;

  private PluginManagerFactory() {}

  /**
   * Creates a {@link DefaultPluginManager} rooted at {@code pluginsDir} and starts all plugins.
   *
   * <p>{@link DefaultPluginManager} is used (not {@code JarPluginManager}) because it includes
   * {@code DefaultPluginRepository}, which automatically extracts ZIP files to directories before
   * loading. {@code JarPluginManager} only handles plain {@code .jar} files.
   *
   * @param pluginsDir directory containing the {@code plugwerk-client-plugin-*.zip}
   * @return started plugin manager
   */
  public static PluginManager create(Path pluginsDir) {
    log.debug(
        "Starting PF4J plugin manager with plugins directory: {}", pluginsDir.toAbsolutePath());

    DefaultPluginManager manager = new DefaultPluginManager(pluginsDir.toAbsolutePath());
    manager.loadPlugins();
    manager.startPlugins();

    log.debug(
        "Loaded plugins: {}",
        manager.getPlugins().stream()
            .map(p -> p.getPluginId() + "@" + p.getDescriptor().getVersion())
            .toList());

    return manager;
  }

  /**
   * Opens a fresh {@link PlugwerkMarketplace} connection backed by {@code config}.
   *
   * <p>The returned marketplace is owned by the caller; close it (or use try-with-resources) to
   * release the underlying HTTP client.
   *
   * @param manager a started {@link PluginManager} with the SDK plugin loaded
   * @param config server URL, namespace, credentials, and plugin directory
   * @return a new {@link PlugwerkMarketplace} connection
   * @throws IllegalStateException if {@code plugwerk-client-plugin} is not loaded
   */
  public static PlugwerkMarketplace getMarketplace(PluginManager manager, PlugwerkConfig config) {
    PluginWrapper wrapper = manager.getPlugin(PLUGIN_ID);
    if (wrapper == null) {
      throw new IllegalStateException(
          """
                    No '%s' plugin found.
                    Make sure plugwerk-client-plugin-<version>.zip is present in the plugins directory.
                    """
              .formatted(PLUGIN_ID));
    }
    return ((PlugwerkPlugin) wrapper.getPlugin()).connect(config);
  }
}
