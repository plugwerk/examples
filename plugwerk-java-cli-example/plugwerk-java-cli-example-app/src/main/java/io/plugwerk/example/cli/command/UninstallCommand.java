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

import io.plugwerk.example.cli.PlugwerkCli;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Unloads and removes an installed plugin.
 *
 * <p>Usage:
 *
 * <pre>
 *   plugwerk-cli uninstall io.example.my-plugin
 * </pre>
 */
@Command(
    name = "uninstall",
    description = "Unload and remove an installed plugin.",
    mixinStandardHelpOptions = true)
public class UninstallCommand implements Runnable {

  @ParentCommand private PlugwerkCli parent;

  @Parameters(index = "0", description = "Plugin ID to remove (e.g. io.example.my-plugin)")
  private String pluginId;

  @Override
  public void run() {
    System.out.printf("Uninstalling %s …%n", pluginId);

    // The installer SPI stops, unloads and deletes the plugin in one call —
    // the host no longer needs to drive the PF4J lifecycle by hand.
    parent
        .getMarketplace()
        .installer()
        .uninstall(pluginId)
        .onSuccess(s -> System.out.printf("✓ Successfully uninstalled %s%n", s.getPluginId()))
        .onFailure(
            f -> {
              System.err.printf("✗ Uninstall failed: %s%n", f.getReason());
              System.exit(1);
            });
  }
}
