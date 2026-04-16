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
import io.plugwerk.spi.extension.PlugwerkMarketplace;
import io.plugwerk.spi.model.UpdateInfo;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.pf4j.PluginWrapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Checks for available plugin updates and optionally installs them.
 *
 * <p>Usage:
 *
 * <pre>
 *   plugwerk-cli update           # check only, print available updates
 *   plugwerk-cli update --apply   # check and install all available updates
 * </pre>
 */
@Command(
    name = "update",
    description = "Check for available plugin updates and optionally install them.",
    mixinStandardHelpOptions = true)
public class UpdateCommand implements Runnable {

  @ParentCommand private PlugwerkCli parent;

  @Option(
      names = {"--apply"},
      description = "Install all available updates (default: check only)")
  private boolean apply;

  @Override
  public void run() {
    PlugwerkMarketplace marketplace = parent.getMarketplace();

    // Build a map of installed plugin ID → current version from PF4J plugin manager
    Map<String, String> installed =
        parent.getPluginManager().getPlugins().stream()
            .collect(
                Collectors.toMap(
                    PluginWrapper::getPluginId, pw -> pw.getDescriptor().getVersion()));

    if (installed.isEmpty()) {
      System.out.println("No plugins currently installed.");
      return;
    }

    List<UpdateInfo> updates = marketplace.updateChecker().checkForUpdates(installed);

    if (updates.isEmpty()) {
      System.out.println("All plugins are up-to-date.");
      return;
    }

    System.out.printf("%-40s %-12s → %s%n", "PLUGIN ID", "CURRENT", "AVAILABLE");
    System.out.println("-".repeat(70));
    for (UpdateInfo u : updates) {
      System.out.printf(
          "%-40s %-12s → %s%n", u.getPluginId(), u.getCurrentVersion(), u.getAvailableVersion());
    }
    System.out.println();

    if (!apply) {
      System.out.printf("%d update(s) available. Run with --apply to install.%n", updates.size());
      return;
    }

    System.out.println("Applying updates …");
    int[] counts = {0, 0}; // [success, failed]
    for (UpdateInfo u : updates) {
      marketplace
          .installer()
          .install(u.getPluginId(), u.getAvailableVersion())
          .onSuccess(
              s -> {
                System.out.printf(
                    "  ✓ %s %s → %s%n", s.getPluginId(), u.getCurrentVersion(), s.getVersion());
                counts[0]++;
              })
          .onFailure(
              f -> {
                System.err.printf("  ✗ %s: %s%n", f.getPluginId(), f.getReason());
                counts[1]++;
              });
    }
    System.out.printf("%nDone: %d updated, %d failed.%n", counts[0], counts[1]);
    if (counts[1] > 0) {
      System.exit(1);
    }
  }
}
