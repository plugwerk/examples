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
package io.plugwerk.example.webapp.controller;

import io.plugwerk.example.webapp.api.PageContribution;
import io.plugwerk.example.webapp.config.PluginContributionRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Routes dynamic page requests to the matching {@link PageContribution} extension.
 *
 * <p>This controller has a low priority (catch-all {@code /{route}}) and only matches routes that
 * are registered by installed plugins. Unknown routes result in a 404.
 */
@Controller
public class DynamicRouteController {

  private final PluginContributionRegistry registry;

  public DynamicRouteController(PluginContributionRegistry registry) {
    this.registry = registry;
  }

  @GetMapping("/page/{route}")
  public String dynamicPage(@PathVariable String route, Model model) {
    PageContribution contribution =
        registry
            .findByRoute(route)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No plugin page found for route: " + route));

    model.addAttribute("contributions", registry.getContributions());
    model.addAttribute("pageTitle", contribution.getTitle());
    model.addAttribute("content", contribution.renderHtml());

    return "dynamic-page";
  }
}
