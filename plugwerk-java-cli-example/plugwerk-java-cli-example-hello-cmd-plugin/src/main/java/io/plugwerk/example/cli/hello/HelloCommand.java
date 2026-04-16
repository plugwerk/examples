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
package io.plugwerk.example.cli.hello;

import io.plugwerk.example.cli.api.CliCommand;
import org.pf4j.Extension;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Example CLI command contributed dynamically via the Plugwerk server.
 *
 * <p>After uploading {@code hello-cli-plugin-<version>.zip} to the server and installing it via
 * {@code cli install hello-cli-plugin <version>}, this subcommand becomes available in the host
 * application:
 *
 * <pre>
 *   cli hello --name=World
 *   Hello, World!
 * </pre>
 */
@Extension
@Command(
    name = "hello",
    description = "Greets the specified name (or the world by default).",
    mixinStandardHelpOptions = true)
public class HelloCommand implements CliCommand, Runnable {

  @Option(
      names = {"--name", "-n"},
      description = "Name to greet (default: ${DEFAULT-VALUE})",
      defaultValue = "World")
  private String name;

  @Option(
      names = {"--language", "-l"},
      description = "Language for the greeting: en, de, es (default: ${DEFAULT-VALUE})",
      defaultValue = "en")
  private String language;

  @Override
  public CommandLine toCommandLine() {
    return new CommandLine(this);
  }

  @Override
  public void run() {
    String greeting =
        switch (language.toLowerCase()) {
          case "de" -> "Hallo, " + name + "!";
          case "es" -> "Hola, " + name + "!";
          default -> "Hello, " + name + "!";
        };
    System.out.println(greeting);
  }
}
