package io.thestencil.client.spi.builders;

/*-
 * #%L
 * stencil-client
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.resys.thena.docdb.api.actions.CommitActions.HeadCommitBuilder;
import io.thestencil.client.api.ImmutableArticle;
import io.thestencil.client.api.ImmutableEntity;
import io.thestencil.client.api.ImmutableLink;
import io.thestencil.client.api.ImmutableLocale;
import io.thestencil.client.api.ImmutablePage;
import io.thestencil.client.api.ImmutableWorkflow;
import io.thestencil.client.api.MigrationBuilder.LocalizedSite;
import io.thestencil.client.api.MigrationBuilder.Sites;
import io.thestencil.client.api.MigrationBuilder.Topic;
import io.thestencil.client.api.MigrationBuilder.TopicBlob;
import io.thestencil.client.api.MigrationBuilder.TopicLink;
import io.thestencil.client.api.StencilClient.Article;
import io.thestencil.client.api.StencilClient.Entity;
import io.thestencil.client.api.StencilClient.EntityType;
import io.thestencil.client.api.StencilClient.Link;
import io.thestencil.client.api.StencilClient.Locale;
import io.thestencil.client.api.StencilClient.Page;
import io.thestencil.client.api.StencilClient.Workflow;
import io.thestencil.client.spi.PersistenceConfig;

public class MigrationImportVisitor {
  private final PersistenceConfig config;
  private final HeadCommitBuilder commit;
  private final Map<String, Entity<Article>> articlesByTopicId = new HashMap<>();
  private final Map<String, Entity<Link>> links = new HashMap<>();
  private final Map<String, Entity<Workflow>> workflows = new HashMap<>();
  
  public MigrationImportVisitor(PersistenceConfig config) {
    super();
    this.config = config;
    this.commit = this.config.getClient().commit().head();
  }
  
  public HeadCommitBuilder visit(Sites sites) {
    
    for(LocalizedSite site :  sites.getSites().values()) {
      Entity<Locale> locale = visitLocale(site);
      
      
      for(Topic topic : site.getTopics().values()) {
        
        Entity<Article> article = visitArticle(locale, topic, site);
        visitPage(article, locale, site.getBlobs().get(topic.getBlob()));
        
        for(String topicLinkId : topic.getLinks()) {
          TopicLink topicLink = site.getLinks().get(topicLinkId);
          
          if(topicLink.getType().equalsIgnoreCase("workflow") || 
              topicLink.getType().equalsIgnoreCase("dialob")) {
            
            visitWorkflow(topicLink, locale, article);
          } else {
            visitLink(topicLink, locale, article);
          }
        }
      }
    }
    
    for(final var entity : links.values()) {
      commit.append(entity.getId(), config.getSerializer().toString(entity));
    }

    for(final var entity : workflows.values()) {
      commit.append(entity.getId(), config.getSerializer().toString(entity));
    }
    return commit;
  }
  
  private Entity<Workflow> visitWorkflow(TopicLink topicLink, Entity<Locale> locale, Entity<Article> article) {
    final var topicLinkId = topicLinkId(topicLink, locale);
    final List<String> articles = new ArrayList<>();

    final String gid;
    if(links.containsKey(topicLinkId)) {
      final var created = links.get(topicLinkId);
      gid = created.getId();
      articles.addAll(created.getBody().getArticles());
      if(!articles.contains(article.getId())) {
        articles.add(article.getId());
      }
    } else {
      gid = gid(EntityType.WORKFLOW);
      articles.add(article.getId());
    }
    
    final var workflow = ImmutableWorkflow.builder()
      .name(topicLink.getName())
      .locale(locale.getId())
      .content(topicLink.getValue())
      .articles(Boolean.TRUE.equals(topicLink.getGlobal()) ? Collections.emptyList() : articles)
      .build();
    final Entity<Workflow> entity = ImmutableEntity.<Workflow>builder()
        .id(gid)
        .type(EntityType.WORKFLOW)
        .body(workflow)
        .build();
    workflows.put(topicLinkId, entity);
    return entity;
  }

  private Entity<Link> visitLink(TopicLink topicLink, Entity<Locale> locale, Entity<Article> article) {
    final var topicLinkId = topicLinkId(topicLink, locale);
    final List<String> articles = new ArrayList<>();
    
    final String gid;
    if(links.containsKey(topicLinkId)) {
      
      final var created = links.get(topicLinkId);
      gid = created.getId();
      articles.addAll(created.getBody().getArticles());
      if(!articles.contains(article.getId())) {
        articles.add(article.getId());
      }
      
    } else {
      gid = gid(EntityType.LINK);
      articles.add(article.getId());
    }
    
    
    final var link = ImmutableLink.builder()
      .description(topicLink.getName())
      .locale(locale.getId())
      .contentType(topicLink.getType())
      .content(topicLink.getValue())
      .articles(Boolean.TRUE.equals(topicLink.getGlobal()) ? Collections.emptyList() : articles)
      .build();
    
    final Entity<Link> entity = ImmutableEntity.<Link>builder()
      .id(gid)
      .type(EntityType.LINK)
      .body(link)
      .build();
    links.put(topicLinkId, entity);
    
    return entity;
  }
  
  private String topicLinkId(TopicLink topicLink, Entity<Locale> locale) {
    return topicLink.getType() + "::" + locale.getId() + "::" + topicLink.getName() + "/" + topicLink.getValue() + "/";
    
  }
  
  private Entity<Page> visitPage(Entity<Article> article, Entity<Locale> locale, TopicBlob topic) {
    final var gid = gid(EntityType.PAGE);
    final var page = ImmutablePage.builder()
        .article(article.getId())
        .locale(locale.getId())
        .content(Optional.ofNullable(topic.getValue()).orElse(""))
        .build();
    
    final Entity<Page> entity = ImmutableEntity.<Page>builder()
        .id(gid)
        .type(EntityType.PAGE)
        .body(page)
        .build();
    commit.append(gid, config.getSerializer().toString(entity));
    return entity;
  }
  
  private Entity<Article> visitArticle(Entity<Locale> locale, Topic topic, LocalizedSite site) {
    if(articlesByTopicId.containsKey(topic.getId())) {
      return articlesByTopicId.get(topic.getId());
    }

    final String parentId;
    String name = null;
    if(topic.getParent() != null && topic.getId().startsWith(topic.getParent())) {
      name = topic.getId().substring(topic.getParent().length() + 1);
      parentId = visitArticle(locale, site.getTopics().get(topic.getParent()), site).getId();
    } else {
      name = topic.getId();
      parentId = null;
    }
    
    int order = 0;    
    try {
      order = Integer.parseInt(name.substring(0, 3));
      name = name.substring(4);
    } catch(Exception e) { }
    
    final var gid = gid(EntityType.ARTICLE);
    final var article = ImmutableArticle.builder()
        .name(name)
        .parentId(parentId)
        .order(order)
        .build();
    final Entity<Article> entity = ImmutableEntity.<Article>builder()
        .id(gid)
        .type(EntityType.ARTICLE)
        .body(article)
        .build();
    
    articlesByTopicId.put(topic.getId(), entity);
    commit.append(gid, config.getSerializer().toString(entity));
    return entity;
  }
  
  private Entity<Locale> visitLocale(LocalizedSite site) {
    final var gid = gid(EntityType.LOCALE);
    final var locale = ImmutableLocale.builder()
        .value(site.getLocale())
        .enabled(true)
        .build();
    
    final Entity<Locale> entity = ImmutableEntity.<Locale>builder()
        .id(gid)
        .type(EntityType.LOCALE)
        .body(locale)
        .build();
    
    commit.append(gid, config.getSerializer().toString(entity));
    return entity;
  }
  
  private String gid(EntityType type) {
    return config.getGidProvider().getNextId(type);
  }

}