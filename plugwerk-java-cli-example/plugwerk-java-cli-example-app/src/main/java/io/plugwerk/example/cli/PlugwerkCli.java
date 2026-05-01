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

import io.plugwerk.example.cli.command.InstallCommand;
import io.plugwerk.example.cli.command.ListCommand;
import io.plugwerk.example.cli.command.SearchCommand;
import io.plugwerk.example.cli.command.UninstallCommand;
import io.plugwerk.example.cli.command.UpdateCommand;
import io.plugwerk.spi.PlugwerkConfig;
import io.plugwerk.spi.extension.PlugwerkMarketplace;
import java.nio.file.Path;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Root picocli command for the Plugwerk CLI example.
 *
 * <p>Global connection options ({@code --server}, {@code --namespace}, {@code --plugins-dir}) are
 * declared here and accessible by all subcommands via {@code @ParentCommand}. The PF4J plugin
 * manager and the {@link PlugwerkMarketplace} facade are initialized lazily on the first call to
 * {@link #getMarketplace()}.
 *
 * <p>Usage:
 *
 * <pre>
 *   plugwerk-cli [--server=URL] [--namespace=NS] [--plugins-dir=DIR] &lt;subcommand&gt; [args...]
 * </pre>
 */
@Command(
    name = "plugwerk-cli",
    mixinStandardHelpOptions = true,
    version = "plugwerk-cli 0.1.0-SNAPSHOT",
    description = "CLI for managing PF4J plugins via the Plugwerk marketplace.",
    subcommands = {
      ListCommand.class,
      SearchCommand.class,
      InstallCommand.class,
      UninstallCommand.class,
      UpdateCommand.class,
      CommandLine.HelpCommand.class,
    })
public class PlugwerkCli implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(PlugwerkCli.class);

  // Picocli's ${VAR} syntax resolves system properties; the ${env:VAR} prefix
  // is required to read environment variables. The plain `${VAR:-default}`
  // form previously used here silently fell through to the literal default
  // because no PLUGWERK_* system property is ever set.
  @Option(
      names = {"--server", "-s"},
      description =
          "Plugwerk server base URL (env: PLUGWERK_SERVER_URL, default: ${DEFAULT-VALUE})",
      defaultValue = "${env:PLUGWERK_SERVER_URL:-http://localhost:8080}")
  public String serverUrl;

  @Option(
      names = {"--namespace", "-n"},
      description = "Namespace slug (env: PLUGWERK_NAMESPACE, default: ${DEFAULT-VALUE})",
      defaultValue = "${env:PLUGWERK_NAMESPACE:-default}")
  public String namespace;

  @Option(
      names = {"--plugins-dir"},
      description = "PF4J plugins directory (env: PLUGWERK_PLUGINS_DIR, default: ${DEFAULT-VALUE})",
      defaultValue = "${env:PLUGWERK_PLUGINS_DIR:-./plugins}")
  public Path pluginsDir;

  @Option(
      names = {"--api-key", "-k"},
      description = "Namespace-scoped API key for authentication (env: PLUGWERK_API_KEY)",
      defaultValue = "${env:PLUGWERK_API_KEY:-}")
  public String apiKey;

  // Lazily initialized on first subcommand invocation
  private PluginManager pluginManager;
  private PlugwerkMarketplace marketplace;

  // Set by Main so that subcommands can trigger --help when needed
  private CommandLine commandLine;

  /**
   * Returns the {@link PlugwerkMarketplace} facade, initializing the PF4J plugin manager and
   * opening the marketplace connection on first call. If {@link #setPluginManager(PluginManager)}
   * was already called (eager startup init), the existing manager is reused.
   *
   * <p>The marketplace is closed automatically by the JVM shutdown hook registered on first
   * initialization.
   *
   * @return the marketplace facade connected to the configured Plugwerk server
   */
  public synchronized PlugwerkMarketplace getMarketplace() {
    if (marketplace == null) {
      if (pluginManager == null) {
        pluginManager = PluginManagerFactory.create(pluginsDir);
        registerShutdownHook();
      }
      marketplace = PluginManagerFactory.getMarketplace(pluginManager, buildConfig());
    }
    return marketplace;
  }

  private PlugwerkConfig buildConfig() {
    PlugwerkConfig.Builder builder =
        new PlugwerkConfig.Builder(serverUrl, namespace)
            .pluginDirectory(pluginsDir.toAbsolutePath());
    if (apiKey != null && !apiKey.isBlank()) {
      builder.apiKey(apiKey);
    }
    return builder.build();
  }

  /**
   * Returns the plugin manager; {@code null} if neither eager init nor {@link #getMarketplace()}
   * was called.
   */
  public PluginManager getPluginManager() {
    return pluginManager;
  }

  /**
   * Pre-sets the plugin manager created during eager startup initialization. Must be called before
   * {@link #getMarketplace()} to avoid double initialization.
   */
  public synchronized void setPluginManager(PluginManager pm) {
    if (this.pluginManager == null) {
      this.pluginManager = pm;
      registerShutdownHook();
    }
  }

  public void setCommandLine(CommandLine commandLine) {
    this.commandLine = commandLine;
  }

  public CommandLine getCommandLine() {
    return commandLine;
  }

  @Override
  public void run() {
    // Called when plugwerk-cli is invoked without a subcommand — print help
    commandLine.usage(System.out);
  }

  private void registerShutdownHook() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (marketplace != null) {
                    log.debug("Closing Plugwerk marketplace");
                    try {
                      marketplace.close();
                    } catch (Exception e) {
                      log.debug("Suppressed exception during marketplace close", e);
                    }
                  }
                  if (pluginManager != null) {
                    log.debug("Stopping PF4J plugin manager");
                    try {
                      pluginManager.stopPlugins();
                    } catch (Exception e) {
                      // PF4J 3.15 has a known bug where stopPlugins() causes a
                      // ConcurrentModificationException during JVM shutdown. Safe to ignore.
                      log.debug("Suppressed exception during plugin manager shutdown", e);
                    }
                  }
                }));
  }
}
