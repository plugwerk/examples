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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Exposes Plugwerk connection info to all Thymeleaf templates. */
@ControllerAdvice
public class GlobalModelAttributes {

  @Value("${plugwerk.server-url:http://localhost:8080}")
  private String serverUrl;

  @Value("${plugwerk.namespace:default}")
  private String namespace;

  @Value("${plugwerk.api-key:}")
  private String apiKey;

  @ModelAttribute("plugwerkServerUrl")
  public String serverUrl() {
    return serverUrl;
  }

  @ModelAttribute("plugwerkNamespace")
  public String namespace() {
    return namespace;
  }

  @ModelAttribute("plugwerkApiKey")
  public String apiKey() {
    return apiKey;
  }
}
