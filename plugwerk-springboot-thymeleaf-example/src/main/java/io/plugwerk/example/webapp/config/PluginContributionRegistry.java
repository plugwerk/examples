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

import io.plugwerk.example.webapp.api.PageContribution;
import java.util.List;
import java.util.Optional;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry of {@link PageContribution} extensions discovered from installed PF4J plugins.
 *
 * <p>Call {@link #refresh()} after installing or uninstalling a plugin to re-scan extensions.
 */
@Component
public class PluginContributionRegistry {

  private static final Logger log = LoggerFactory.getLogger(PluginContributionRegistry.class);

  private final PluginManager pluginManager;
  private volatile List<PageContribution> contributions;

  public PluginContributionRegistry(PluginManager pluginManager) {
    this.pluginManager = pluginManager;
    refresh();
  }

  /** Re-scans all installed plugins for {@link PageContribution} extensions. */
  public void refresh() {
    contributions = List.copyOf(pluginManager.getExtensions(PageContribution.class));
    log.info(
        "Discovered {} page contribution(s): {}",
        contributions.size(),
        contributions.stream().map(c -> c.getRoute() + " (" + c.getMenuLabel() + ")").toList());
  }

  /** Returns all currently registered page contributions. */
  public List<PageContribution> getContributions() {
    return contributions;
  }

  /** Finds a page contribution by its route segment. */
  public Optional<PageContribution> findByRoute(String route) {
    return contributions.stream().filter(c -> c.getRoute().equals(route)).findFirst();
  }
}
