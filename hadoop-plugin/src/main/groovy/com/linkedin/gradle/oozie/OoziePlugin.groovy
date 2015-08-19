/*
 * Copyright 2015 LinkedIn Corp.
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
package com.linkedin.gradle.oozie;

import com.linkedin.gradle.hadoopdsl.HadoopDslChecker;
import com.linkedin.gradle.hadoopdsl.HadoopDslFactory;
import com.linkedin.gradle.hadoopdsl.HadoopDslPlugin;

import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

/**
 * OoziePlugin implements features for Apache Oozie, including building the Hadoop DSL for Oozie.
 */
class OoziePlugin implements Plugin<Project> {
  /**
   *  Applies the OoziePlugin.
   *
   *  @param project The Gradle project
   */
  @Override
  void apply(Project project) {
    // Enable users to skip the plugin
    if (project.hasProperty("disableOoziePlugin")) {
      println("OoziePlugin disabled");
      return;
    }

    createBuildFlowsTask(project);
    createOozieUploadTask(project);
    createWriteOoziePluginJsonTask(project);
  }

  /**
   * Creates the task to build the Hadoop DSL for Oozie.
   *
   * @param project The Gradle project
   * @returns The created task
   */
  Task createBuildFlowsTask(Project project) {
    return project.tasks.create("buildOozieFlows") {
      description = "Builds the Hadoop DSL for Apache Oozie. Have your build task depend on this task.";
      group = "Hadoop Plugin";

      doLast {
        HadoopDslPlugin plugin = project.extensions.hadoopDslPlugin;
        HadoopDslFactory factory = project.extensions.hadoopDslFactory;

        // Run the static checker on the DSL
        HadoopDslChecker checker = factory.makeChecker(project);
        checker.check(plugin);

        if (checker.failedCheck()) {
          throw new Exception("Hadoop DSL static checker FAILED");
        }
        else {
          logger.lifecycle("Hadoop DSL static checker PASSED");
        }

        OozieDslCompiler compiler = makeCompiler(project);
        compiler.compile(plugin);
      }
    }
  }

  /**
   * Creates the oozieUpload task.
   *
   * @param project The Gradle project
   * @return The created task
   */
  Task createOozieUploadTask(Project project) {
    return project.tasks.create(name: "oozieUpload", type: OozieUploadTask) { task ->
      description = "Uploads the Oozie project folder to HDFS.";
      group = "Hadoop Plugin";

      doFirst{
        oozieProject = readOozieProject(project);
      }
    }
  }

  /**
   * Creates the writeOoziePluginJson task.
   *
   * @param project The Gradle project
   * @return The created task
   */
  Task createWriteOoziePluginJsonTask(Project project) {
    return project.tasks.create("writeOoziePluginJson") {
      description = "Creates a .ooziePlugin.json file in the project directory with default properties."
      group = "Hadoop plugin";

      doLast {
        def ooziePluginFilePath = "${project.getProjectDir()}/.ooziePlugin.json";
        if(!new File(ooziePluginFilePath).exists()) {
          String oozieData = new JsonBuilder(new OozieProject()).toPrettyString();
          new File(ooziePluginFilePath).write(oozieData);
        }
      }
    }
  }

  /**
   * Helper method to determine the location of the plugin json file. This helper method will make
   * it easy for subclasses to get (or customize) the file location.
   *
   * @param project The Gradle project
   * @return The path to the plugin json file
   */
  String getPluginJsonPath(Project project) {
    return "${project.getProjectDir()}/.ooziePlugin.json";
  }

  /**
   * Factory method to build the Hadoop DSL compiler for Apache Oozie. Subclasses can override this
   * method to provide their own compiler.
   *
   * @param project The Gradle project
   * @return The OozieDslCompiler
   */
  OozieDslCompiler makeCompiler(Project project) {
    return new OozieDslCompiler(project);
  }

  /**
   * Helper method to read the plugin json file as a JSON object.
   *
   * @param project The Gradle project
   * @return A JSON object or null if the file does not exist
   */
  def readOoziePluginJson(Project project) {
    String pluginJsonPath = getPluginJsonPath(project);
    if (!new File(pluginJsonPath).exists()) {
      return null;
    }

    def reader = null;
    try {
      reader = new BufferedReader(new FileReader(pluginJsonPath));
      def slurper = new JsonSlurper();
      def pluginJson = slurper.parse(reader);
      return pluginJson;
    }
    catch (Exception ex) {
      throw new Exception("\nError parsing ${pluginJsonPath}.\n" + ex.toString());
    }
    finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  /**
   * Loads the Oozie project properties defined in the .ooziePlugin.json file.
   *
   * @return An OozieProject object with the properties set
   */
  OozieProject readOozieProject(Project project) {
    def pluginJson = readOoziePluginJson(project);
    if (pluginJson == null) {
      throw new GradleException("\n\nPlease run \"gradle writeOoziePluginJson\" to create a default .ooziePlugin.json file in your project directory which you can then edit.\n")
    }

    OozieProject oozieProject = new OozieProject();
    oozieProject.clusterURI = pluginJson[OozieConstants.OOZIE_CLUSTER_URI]
    oozieProject.uploadPath = pluginJson[OozieConstants.PATH_TO_UPLOAD]
    oozieProject.projectName = pluginJson[OozieConstants.OOZIE_PROJECT_NAME]
    oozieProject.dirToUpload = pluginJson[OozieConstants.DIR_TO_UPLOAD]
    return oozieProject;
  }
}