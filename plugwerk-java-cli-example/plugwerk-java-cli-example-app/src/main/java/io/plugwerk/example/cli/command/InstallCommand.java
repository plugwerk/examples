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
package io.plugwerk.example.cli.command;

import io.plugwerk.example.cli.DynamicCommandLoader;
import io.plugwerk.example.cli.PlugwerkCli;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.pf4j.PluginManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Downloads and installs a plugin from the Plugwerk server.
 *
 * <p>Usage:
 *
 * <pre>
 *   plugwerk-cli install io.example.my-plugin 1.2.0
 * </pre>
 *
 * <p>After a successful install, any {@link io.plugwerk.example.cli.api.CliCommand} extensions
 * provided by the installed plugin are registered as new subcommands automatically.
 */
@Command(
    name = "install",
    description = "Download and install a plugin from the Plugwerk server.",
    mixinStandardHelpOptions = true)
public class InstallCommand implements Runnable {

  @ParentCommand private PlugwerkCli parent;

  @Parameters(index = "0", description = "Plugin ID (e.g. io.example.my-plugin)")
  private String pluginId;

  @Parameters(index = "1", description = "Version to install (e.g. 1.2.0)")
  private String version;

  @Override
  public void run() {
    System.out.printf("Installing %s@%s …%n", pluginId, version);

    parent
        .getMarketplace()
        .installer()
        .install(pluginId, version)
        .onSuccess(
            s -> {
              System.out.printf(
                  "✓ Successfully installed %s@%s%n", s.getPluginId(), s.getVersion());
              // Load and start only the newly installed plugin, then register its extensions.
              // Using loadPlugin(path) instead of loadPlugins() avoids a
              // PluginAlreadyLoadedException
              // for plugins that are already running (e.g. plugwerk-client-plugin).
              PluginManager pm = parent.getPluginManager();
              if (pm != null) {
                Path artifact = findInstalledArtifact(parent.pluginsDir, pluginId, version);
                if (artifact != null) {
                  pm.loadPlugin(artifact);
                }
                pm.startPlugin(pluginId);
              }
              DynamicCommandLoader.reload(parent.getCommandLine(), pm);
            })
        .onFailure(
            f -> {
              System.err.printf("✗ Installation failed: %s%n", f.getReason());
              System.exit(1);
            });
  }

  private static Path findInstalledArtifact(Path pluginsDir, String pluginId, String version) {
    String prefix = pluginId + "-" + version + ".";
    try (var stream = Files.list(pluginsDir)) {
      return stream
          .filter(
              p -> {
                String name = p.getFileName().toString();
                return name.startsWith(prefix) && (name.endsWith(".zip") || name.endsWith(".jar"));
              })
          .findFirst()
          .orElse(null);
    } catch (IOException e) {
      return null;
    }
  }
}
