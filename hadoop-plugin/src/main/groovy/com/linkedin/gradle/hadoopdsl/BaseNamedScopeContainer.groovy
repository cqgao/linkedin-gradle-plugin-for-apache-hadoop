/*
 * Copyright 2014 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.linkedin.gradle.hadoopdsl;

import org.gradle.api.Project;


/**
 * Base class for DSL elements that create a new scope.
 */
abstract class BaseNamedScopeContainer implements NamedScopeContainer {
  HadoopDslFactory factory;
  NamedScope scope;
  Project project;

  // DSL elements that can be added in scope
  List<Job> jobs;
  List<Properties> properties;
  List<Workflow> workflows;

  /**
   * Constructor for the BaseNamedScopeContainer.
   * <p>
   * This overload is intended to be called from the HadoopDslPlugin constructor, which does not
   * yet have the Gradle project available. Once the plugin's apply method is called, it will go
   * back and set the project-related properties.
   *
   * @param parentScope The parent scope
   * @param scopeName The scope name
   */
  BaseNamedScopeContainer(NamedScope parentScope, String scopeName) {
    this.factory = null;
    this.scope = new NamedScope(scopeName, parentScope);
    this.project = null;
    this.jobs = new ArrayList<Job>();
    this.properties = new ArrayList<Properties>();
    this.workflows = new ArrayList<Job>();
  }

  /**
   * Constructor for the BaseNamedScopeContainer.
   *
   * @param project The Gradle project
   * @param parentScope The parent scope
   * @param scopeName The scope name
   */
  BaseNamedScopeContainer(Project project, NamedScope parentScope, String scopeName) {
    this.factory = project.extensions.hadoopDslFactory;
    this.scope = new NamedScope(scopeName, parentScope);
    this.jobs = new ArrayList<Job>();
    this.project = project;
    this.properties = new ArrayList<Properties>();
    this.workflows = new ArrayList<Job>();
  }

  /**
   * Clones the scope container. DSL elements that extend BaseNamedScopeContainer must provide an
   * actual clone method implementation.
   *
   * {@inheritDoc}
   * @see java.lang.Object#clone()
   */
  @Override
  abstract BaseNamedScopeContainer clone();

  /**
   * Helper method to set the properties on a cloned scope container.
   *
   * @param container The scope container being cloned
   * @return The cloned scope container
   */
  BaseNamedScopeContainer clone(BaseNamedScopeContainer container) {
    container.scope = scope.clone();

    // Clear the scope for the cloned container. Then clone all the jobs,
    // properties and workflows declared in the original container and use
    // them to rebuild the scope.
    container.scope.thisLevel.clear();

    for (Job job : jobs) {
      Job jobClone = job.clone();
      container.jobs.add(jobClone);
      container.scope.bind(jobClone.name, jobClone);
    }

    for (Properties props : properties) {
      Properties propsClone = props.clone();
      container.properties.add(propsClone);
      container.scope.bind(propsClone.name, propsClone);
    }

    for (Workflow workflow : workflows) {
      Workflow workflowClone = workflow.clone();
      container.workflows.add(workflowClone);
      container.scope.bind(workflowClone.name, workflowClone);
    }

    return container;
  }

  /**
   * Helper method to configure a Job in the DSL. Can be called by subclasses to configure custom
   * Job subclass types.
   *
   * @param job The job to configure
   * @param configure The configuration closure
   * @return The input job, which is now configured
   */
  Job configureJob(Job job, Closure configure) {
    Methods.configureJob(project, job, configure, scope);
    jobs.add(job);
    return job;
  }

  /**
   * Helper method to configure Properties objects in the DSL. Can be called by subclasses to
   * configure custom Properties subclass types.
   *
   * @param props The properties to configure
   * @param configure The configuration closure
   * @return The input properties, which is now configured
   */
  Properties configureProperties(Properties props, Closure configure) {
    Methods.configureProperties(project, props, configure, scope);
    properties.add(props);
    return props;
  }

  /**
   * Helper method to configure a Workflow in the DSL. Can be called by subclasses to configure
   * custom Workflow subclass types.
   *
   * @param workflow The workflow to configure
   * @param configure The configuration closure
   * @return The input workflow, which is now configured
   */
  Workflow configureWorkflow(Workflow workflow, Closure configure) {
    Methods.configureWorkflow(project, workflow, configure, scope);
    workflows.add(workflow);
    return workflow;
  }

  /**
   * DSL addJob method. Looks up the job with given name, clones it, configures the clone with the
   * given configuration closure and adds the clone to the workflow.
   *
   * @param name The job name to lookup
   * @param configure The configuration closure
   * @return The cloned and configured job that was added to the workflow
   */
  Job addJob(String name, Closure configure) {
    return configureJob(Methods.cloneJob(name, scope), configure);
  }

  /**
   * DSL addJob method. Looks up the job with given name, clones it, renames the clone to the
   * specified name, configures the clone with the given configuration closure and adds the clone
   * to the workflow.
   *
   * @param name The job name to lookup
   * @param rename The new name to give the cloned job
   * @param configure The configuration closure
   * @return The cloned, renamed and configured job that was added to the workflow
   */
  Job addJob(String name, String rename, Closure configure) {
    return configureJob(Methods.cloneJob(name, rename, scope), configure);
  }

  /**
   * DSL addPropertyFile method. Looks up the properties with given name, clones it, configures the
   * clone with the given configuration closure and binds the clone in scope.
   *
   * @param name The properties name to lookup
   * @param configure The configuration closure
   * @return The cloned and configured properties object that was bound in scope
   */
  Properties addPropertyFile(String name, Closure configure) {
    return configureProperties(Methods.clonePropertyFile(name, scope), configure);
  }

  /**
   * DSL addPropertyFile method. Looks up the properties with given name, clones it, renames the
   * clone to the specified name, configures the clone with the given configuration closure and
   * binds the clone in scope.
   *
   * @param name The properties name to lookup
   * @param rename The new name to give the cloned properties object
   * @param configure The configuration closure
   * @return The cloned, renamed and configured properties object that was bound in scope
   */
  Properties addPropertyFile(String name, String rename, Closure configure) {
    return configureProperties(Methods.clonePropertyFile(name, rename, scope), configure);
  }

  /**
   * DSL addWorkflow method. Looks up the workflow with given name, clones it, configures the clone
   * with the given configuration closure and binds the clone in scope.
   *
   * @param name The workflow name to lookup
   * @param configure The configuration closure
   * @return The cloned and configured workflow that was bound in scope
   */
  Workflow addWorkflow(String name, Closure configure) {
    return configureWorkflow(Methods.cloneWorkflow(name, scope), configure);
  }

  /**
   * DSL addWorkflow method. Looks up the workflow with given name, clones it, renames the clone to
   * the specified name, configures the clone with the given configuration closure and binds the
   * clone in scope.
   *
   * @param name The workflow name to lookup
   * @param rename The new name to give the cloned workflow
   * @param configure The configuration closure
   * @return The cloned, renamed and configured workflow that was bound in scope
   */
  Workflow addWorkflow(String name, String rename, Closure configure) {
    return configureWorkflow(Methods.cloneWorkflow(name, rename, scope), configure);
  }

  /**
   * Returns the scope at this level.
   *
   * @return The scope at this level
   */
  @Override
  public NamedScope getScope() {
    return scope;
  }

  /**
   * DSL lookup method. Looks up an object in scope.
   *
   * @param name The name to lookup
   * @return The object that is bound in scope to the given name, or null if no such name is bound in scope
   */
  Object lookup(String name) {
    return Methods.lookup(name, scope);
  }

  /**
   * DSL lookup method. Looks up an object in scope and then applies the given configuration
   * closure.
   *
   * @param name The name to lookup
   * @param configure The configuration closure
   * @return The object that is bound in scope to the given name, or null if no such name is bound in scope
   */
  Object lookup(String name, Closure configure) {
    return Methods.lookup(project, name, scope, configure);
  }

  /**
   * DSL propertyFile method. Creates a Properties object in scope with the given name and
   * configuration.
   *
   * @param name The properties name
   * @param configure The configuration closure
   * @return The new properties object
   */
  Properties propertyFile(String name, Closure configure) {
    return configureProperties(factory.makeProperties(name), configure);
  }

  /**
   * DSL workflow method. Creates a Workflow in scope with the given name and configuration.
   *
   * @param name The workflow name
   * @param configure The configuration closure
   * @return The new workflow
   */
  Workflow workflow(String name, Closure configure) {
    return configureWorkflow(factory.makeWorkflow(name, project, scope), configure);
  }

  /**
   * DSL job method. Creates an Job in scope with the given name and configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  Job job(String name, Closure configure) {
    return configureJob(factory.makeJob(name), configure);
  }

  /**
   * DSL commandJob method. Creates a CommandJob in scope with the given name and configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  CommandJob commandJob(String name, Closure configure) {
    return configureJob(factory.makeCommandJob(name), configure);
  }

  /**
   * DSL hadoopJavaJob method. Creates a HadoopJavaJob in scope with the given name and
   * configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  HadoopJavaJob hadoopJavaJob(String name, Closure configure) {
    return configureJob(factory.makeHadoopJavaJob(name), configure);
  }

  /**
   * DSL hiveJob method. Creates a HiveJob in scope with the given name and configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  HiveJob hiveJob(String name, Closure configure) {
    return configureJob(factory.makeHiveJob(name), configure);
  }

  /**
   * @deprecated JavaJob has been deprecated in favor of HadoopJavaJob or JavaProcessJob.
   *
   * DSL javaJob method. Creates a JavaJob in scope with the given name and configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  @Deprecated
  JavaJob javaJob(String name, Closure configure) {
    project.logger.lifecycle("JavaJob has been deprecated in favor of HadoopJavaJob or JavaProcessJob. Please change the job ${name} to one of these classes.");
    return configureJob(factory.makeJavaJob(name), configure);
  }

  /**
   * DSL javaProcessJob method. Creates a JavaProcessJob in scope with the given name and
   * configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  JavaProcessJob javaProcessJob(String name, Closure configure) {
    return configureJob(factory.makeJavaProcessJob(name), configure);
  }

  /**
   * DSL kafkaPushJob method. Creates a KafkaPushJob in scope with the given name and
   * configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  KafkaPushJob kafkaPushJob(String name, Closure configure) {
    return configureJob(factory.makeKafkaPushJob(name), configure);
  }

  /**
   * DSL noOpJob method. Creates a NoOpJob in scope with the given name and configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  NoOpJob noOpJob(String name, Closure configure) {
    return configureJob(factory.makeNoOpJob(name), configure);
  }

  /**
   * DSL pigJob method. Creates a PigJob in scope with the given name and configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  PigJob pigJob(String name, Closure configure) {
    return configureJob(factory.makePigJob(name), configure);
  }

  /**
   * DSL voldemortBuildPushJob method. Creates a VoldemortBuildPushJob in scope with the given name
   * and configuration.
   *
   * @param name The job name
   * @param configure The configuration closure
   * @return The new job
   */
  VoldemortBuildPushJob voldemortBuildPushJob(String name, Closure configure) {
    return configureJob(factory.makeVoldemortBuildPushJob(name), configure);
  }
}