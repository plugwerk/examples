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

import org.pf4j.Plugin;

/**
 * PF4J plugin entry point for the hello-cli-plugin.
 *
 * <p>Contributes the {@link HelloCommand} subcommand to the CLI host application via the {@link
 * io.plugwerk.example.cli.api.CliCommand} extension point.
 */
public class HelloPlugin extends Plugin {}
