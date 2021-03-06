package io.thestencil.workflows;

/*-
 * #%L
 * quarkus-stencil-workflows-deployment
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




import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.thestencil.workflows.core.api.ImmutableWorkflow;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class WorkflowsProcessor {
  WorkflowsConfig config;
  
  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(WorkflowsRecorder.FEATURE_BUILD_ITEM);
  }
  
  @BuildStep
  @Record(ExecutionTime.STATIC_INIT)
  void buildtimeInit(
      WorkflowsBuildItem buildItem,
      WorkflowsRecorder recorder,
      BuildProducer<AdditionalBeanBuildItem> buildItems,
      BuildProducer<BeanContainerListenerBuildItem> beans) {
    
    buildItems.produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(WorkflowsProducer.class).build());
    beans.produce(new BeanContainerListenerBuildItem(recorder.configureBuildtimeConfig(
        this.config.mock.enabled,
        this.config.mock.apiKey, 
        this.config.mock.formId,
        "/" + buildItem.getServicePath(),
        "/" + buildItem.getFillPath(),
        "/" + buildItem.getReviewPath()
        )));
  }

  
  @BuildStep
  @Record(ExecutionTime.RUNTIME_INIT)
  void runtimeInit(
      RuntimeConfig config,
      WorkflowsBuildItem buildItem,
      WorkflowsRecorder recorder,
      
      BeanContainerBuildItem beanContainer, 
      ShutdownContextBuildItem shutdown) {
    
    recorder.configureRuntimeConfig(config);
  }
  
  @BuildStep
  @Record(ExecutionTime.RUNTIME_INIT)
  public void staticContentHandler(
    WorkflowsBuildItem buildItem,
    WorkflowsRecorder recorder,
    HttpRootPathBuildItem httpRoot,
    BuildProducer<RouteBuildItem> routes,
    BodyHandlerBuildItem body,
    WorkflowsConfig config) throws Exception {
    
    final var bodyHandler = body.getHandler();
    final Handler<RoutingContext> handler = recorder.userActionsHandler();
    
    routes.produce(httpRoot.routeBuilder()
        .routeFunction(buildItem.getServicePath(), recorder.routeFunction(bodyHandler))
        .handler(handler)
        .build());
    routes.produce(httpRoot.routeBuilder()
        .routeFunction(buildItem.getServicePath() + "/*", recorder.routeFunction(bodyHandler))
        .handler(handler)
        .build());
    
    /* @deprecated way of doing
    routes.produce(new RouteBuildItem.Builder()
        .routeFunction(recorder.routeFunction("/"+ buildItem.getServicePath(), body.getHandler()))
        .handler(handler)
        .blockingRoute()
        .build());
    routes.produce(new RouteBuildItem.Builder()
        .routeFunction(recorder.routeFunction("/"+ buildItem.getServicePath() + "/*", body.getHandler()))
        .handler(handler)
        .blockingRoute()
        .build());*/
  }
  
  @BuildStep
  public ReflectiveClassBuildItem reflection() {
    return new ReflectiveClassBuildItem(true, true, 
        ImmutableWorkflow.class);
  }

  @BuildStep
  @Record(ExecutionTime.STATIC_INIT)
  public void frontendBeans(
      WorkflowsRecorder recorder,
      BuildProducer<WorkflowsBuildItem> buildProducer,
      HttpRootPathBuildItem httpRootPathBuildItem,
      BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints) throws Exception {
    
    final var servicePath = cleanPath(config.servicePath);
    final var buildItem = new WorkflowsBuildItem(
        servicePath, 
        servicePath + "/fill", 
        servicePath + "/review");
    
    displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(httpRootPathBuildItem.resolvePath(servicePath), "User Actions"));
    displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(httpRootPathBuildItem.resolvePath(buildItem.getFillPath()), "User Actions Fill Form"));
    displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(httpRootPathBuildItem.resolvePath(buildItem.getReviewPath()), "User Actions Review Form"));
    buildProducer.produce(buildItem);
  }
  
  private static String cleanPath(String value) {
    return WorkflowsProducer.cleanPath(value);
  }
}
