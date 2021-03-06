package io.thestencil.client.api;

import java.time.LocalDate;

/*-
 * #%L
 * stencil-persistence-api
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

import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.smallrye.mutiny.Uni;
import io.thestencil.client.api.StencilClient.Article;
import io.thestencil.client.api.StencilClient.Entity;
import io.thestencil.client.api.StencilClient.Link;
import io.thestencil.client.api.StencilClient.Locale;
import io.thestencil.client.api.StencilClient.LocaleLabel;
import io.thestencil.client.api.StencilClient.Page;
import io.thestencil.client.api.StencilClient.Release;
import io.thestencil.client.api.StencilClient.SiteState;
import io.thestencil.client.api.StencilClient.Template;
import io.thestencil.client.api.StencilClient.Workflow;

public interface CreateBuilder {
  
  Uni<SiteState> repo();
  Uni<Entity<Article>> article(CreateArticle init);
  Uni<Entity<Release>> release(CreateRelease init);
  Uni<Entity<Locale>> locale(CreateLocale init);
  Uni<Entity<Page>> page(CreatePage init);
  Uni<Entity<Link>> link(CreateLink init);
  Uni<Entity<Workflow>> workflow(CreateWorkflow init);  
  Uni<Entity<Template>> template(CreateTemplate init);  

  

  @Value.Immutable
  @JsonSerialize(as = ImmutableCreateArticle.class)
  @JsonDeserialize(as = ImmutableCreateArticle.class)
  interface CreateArticle {
    @Nullable
    String getParentId();
    String getName();
    @Nullable
    Integer getOrder(); 
  }
  
  @Value.Immutable
  @JsonSerialize(as = ImmutableCreateTemplate.class)
  @JsonDeserialize(as = ImmutableCreateTemplate.class)
  interface CreateTemplate {
	  String getName();
    String getDescription();
	  String getContent();
	  String getType();
  }
  
  
  @Value.Immutable
  @JsonSerialize(as = ImmutableCreateRelease.class)
  @JsonDeserialize(as = ImmutableCreateRelease.class)
  interface CreateRelease {
    String getName();
    @Nullable
    String getNote();
  }
  
  @Value.Immutable
  @JsonSerialize(as = ImmutableCreateLocale.class)
  @JsonDeserialize(as = ImmutableCreateLocale.class)
  interface CreateLocale {
    String getLocale();
  }
  
  @Value.Immutable
  @JsonSerialize(as = ImmutableCreatePage.class)
  @JsonDeserialize(as = ImmutableCreatePage.class)
  interface CreatePage {
    String getArticleId();
    String getLocale();
    @Nullable
    String getContent();
  }
  
  @Value.Immutable
  @JsonSerialize(as = ImmutableCreateLink.class)
  @JsonDeserialize(as = ImmutableCreateLink.class)
  interface CreateLink {
    String getValue(); 
    String getType();
    List<String> getArticles();
    List<LocaleLabel> getLabels();
  }
  
  @Value.Immutable
  @JsonSerialize(as = ImmutableCreateWorkflow.class)
  @JsonDeserialize(as = ImmutableCreateWorkflow.class)
  interface CreateWorkflow {
    String getValue();
    List<String> getArticles();
    List<LocaleLabel> getLabels();
    @Nullable
    Boolean getDevMode();
  }

}
