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

import java.nio.file.Path;
import picocli.CommandLine;

/**
 * Entry point for the Plugwerk Java CLI example.
 *
 * <p>Run via Gradle:
 *
 * <pre>
 * ./gradlew :plugwerk-java-cli-example-app:run \
 *     --args="--server=http://localhost:8080 list"
 * </pre>
 *
 * <p>Or with the fat JAR after {@code ./gradlew assemble}:
 *
 * <pre>
 * java -jar build/libs/plugwerk-java-cli-example-app-*-fat.jar list
 * </pre>
 */
public class Main {

  public static void main(String[] args) {
    PlugwerkCli cli = new PlugwerkCli();
    CommandLine commandLine = new CommandLine(cli);
    cli.setCommandLine(commandLine);

    // Pre-parse global options so that pluginsDir, serverUrl etc. are populated
    // from command-line args and env vars before we initialize the plugin manager.
    // Errors (e.g. unknown subcommand before dynamic commands are registered) are
    // intentionally ignored — execute() will handle them properly.
    boolean helpRequested = false;
    try {
      CommandLine.ParseResult parseResult = commandLine.parseArgs(args);
      helpRequested = parseResult.isUsageHelpRequested() || parseResult.isVersionHelpRequested();
    } catch (Exception ignored) {
    }

    // Short-circuit for --help / --version: print usage without initializing the
    // plugin manager (which requires the plugwerk-client plugin ZIP to be present).
    if (helpRequested) {
      int exitCode = commandLine.execute(args);
      System.exit(exitCode);
      return;
    }

    // Eagerly initialize the plugin manager so that already-installed plugins are
    // loaded and their CliCommand extensions are registered as picocli subcommands
    // before execute() tries to match the user's subcommand name.
    // The first parseArgs() above may have thrown before applying picocli defaults
    // (e.g. when the user invokes a plugin-contributed subcommand that hasn't been
    // registered yet). In that case cli.pluginsDir is still null even though
    // PlugwerkCli's @Option uses ${env:PLUGWERK_PLUGINS_DIR:-./plugins}, so we
    // mirror the same env-or-default lookup manually here.
    Path pluginsDir =
        cli.pluginsDir != null
            ? cli.pluginsDir
            : Path.of(System.getenv().getOrDefault("PLUGWERK_PLUGINS_DIR", "./plugins"));

    org.pf4j.PluginManager pm = PluginManagerFactory.create(pluginsDir);
    cli.setPluginManager(pm);
    DynamicCommandLoader.loadAll(commandLine, pm);

    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }
}
