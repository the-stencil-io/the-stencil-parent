package io.thestencil.staticontent.api;

import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import io.thestencil.client.api.MigrationBuilder.Sites;
import io.thestencil.client.api.StencilClient.SiteState;

/*-
 * #%L
 * stencil-static-content
 * %%
 * Copyright (C) 2021 Copyright 2021 ReSys OÜ
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

public interface StaticContentClient {
  
  MarkdownBuilder markdown();
  SitesBuilder sites();
  
  interface SitesBuilder {
    SitesBuilder imagePath(String imagePath);
    SitesBuilder created(long created);
    SitesBuilder source(Markdowns markdowns);
    Sites build();
  }
  
  interface MarkdownBuilder {
    MarkdownBuilder json(String jsonOfSiteState, boolean dev);
    MarkdownBuilder json(SiteState jsonOfSiteState, boolean dev);
    MarkdownBuilder md(String path, byte[] value);
    Markdowns build();
  }
  
  @Value.Immutable
  interface Markdowns {
    List<Markdown> getValues();
    List<ImageResource> getImages();
    List<LinkResource> getLinks();
    List<String> getLocales();
  }
  @Value.Immutable
  interface LinkResource {
    String getId();
    String getType();
    String getPath();
    String getValue();
    List<String> getLocale();
    Boolean getWorkflow();
    Boolean getGlobal();
    @Nullable
    String getDesc();
  }
  
  @Value.Immutable
  interface ImageResource {
    String getPath();
    byte[] getValue();
  }
  
  @Value.Immutable
  interface Markdown {
    String getLocale();
    String getPath();
    String getValue();
    List<Heading> getHeadings();
    List<ImageTag> getImages();
  }
  
  @Value.Immutable
  interface Heading {
    Integer getOrder();
    Integer getLevel();
    String getName();
  }
  
  @Value.Immutable
  interface ImageTag {
    Integer getLine();
    String getTitle();
    String getAltText();
    String getPath();
  }
}
