package com.byrcegao.buildsrc

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class TPPlugin  implements Plugin<Project> {
  @Override
  void apply(Project project) {
    println("this is TPPlugin")

    def config = project.extensions.create("tpconfig", TPConfig) //配置信息

    def android = project.extensions.findByType(AppExtension)
    android.registerTransform(new TPTransform(project, config))
  }
}