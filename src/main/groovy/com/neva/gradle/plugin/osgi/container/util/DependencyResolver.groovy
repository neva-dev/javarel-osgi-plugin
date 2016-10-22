package com.neva.gradle.plugin.osgi.container.util

import com.neva.gradle.plugin.osgi.container.ContainerConfig
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency

final class DependencyResolver {

    static Collection<File> jars(Project project) {
        def moduleConfig = project.configurations.getByName(ContainerConfig.MODULE)
        def moduleDeps = spread(moduleConfig)
        def moduleJars = moduleConfig.resolve()

        def allBundles = new HashSet<>(moduleJars)

        moduleDeps.each { ProjectDependency projectDependency ->
            def subProject = projectDependency.dependencyProject
            def subBundles = subProject.configurations.getByName(ContainerConfig.BUNDLE).resolve()

            allBundles += subBundles
        }

        return allBundles
    }

    static Collection<ProjectDependency> spread(Configuration config) {
        def projDeps = config.allDependencies.findAll {
            it instanceof ProjectDependency
        } as Collection<ProjectDependency>

        return flatten(projDeps)
    }

    static Collection<ProjectDependency> flatten(Collection<ProjectDependency> projDeps) {
        def result = new HashSet<ProjectDependency>(projDeps)
        for (ProjectDependency projDep : projDeps) {
            def deps = spread(projDep)
            result.addAll(deps)
        }

        return result
    }

    static Collection<ProjectDependency> spread(ProjectDependency projDep) {
        def projDeps = projDep.projectConfiguration.allDependencies.findAll {
            it instanceof ProjectDependency
        } as Collection<ProjectDependency>

        return flatten(projDeps)
    }

}
