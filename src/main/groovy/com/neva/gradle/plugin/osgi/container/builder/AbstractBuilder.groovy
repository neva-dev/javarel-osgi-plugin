package com.neva.gradle.plugin.osgi.container.builder

import com.neva.gradle.plugin.osgi.container.ContainerConfig
import com.neva.gradle.plugin.osgi.container.ContainerException
import com.neva.gradle.plugin.osgi.container.ContainerExtension
import com.neva.gradle.plugin.osgi.container.util.BundleDetector
import com.neva.gradle.plugin.osgi.container.util.BundleWrapper
import com.neva.gradle.plugin.osgi.container.util.MapStringifier
import com.neva.gradle.plugin.osgi.container.util.DependencyResolver
import groovy.text.SimpleTemplateEngine
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileTreeElement

class AbstractBuilder implements ContainerBuilder {

    static final FILE_ENCODING = 'UTF-8'

    Project project

    AbstractBuilder(Project project) {
        this.project = project
    }

    @Override
    def main() {
        def config = project.configurations.getByName(ContainerConfig.MAIN)
        def size = config.dependencies.size()

        if (size == 0) {
            throw new ContainerException("Configuration named '${ContainerConfig.MAIN}' should have container main dependency defined.")
        } else if (size != 1) {
            throw new ContainerException("Configuration named '${ContainerConfig.MAIN}' cannot have more than one dependency defined.")
        }

        project.copy {
            from config
            into extension.containerDir
        }
    }

    @Override
    def bundles() {
        def moduleConfig = project.configurations.getByName(ContainerConfig.MODULE)
        def moduleDeps = DependencyResolver.spread(moduleConfig)
        def moduleJars = moduleConfig.resolve()

        def allBundles = new HashSet<>(moduleJars)

        moduleDeps.each { ProjectDependency projectDependency ->
            def subProject = projectDependency.dependencyProject
            def subBundles = subProject.configurations.getByName(ContainerConfig.BUNDLE).resolve()

            allBundles += subBundles
        }

        copyBundles(allBundles)
    }

    def copyBundles(Collection<File> files) {
        def nonBundles = []
        def excluded = files.findAll { file ->
            extension.exclusions.any { exclusion ->
                FilenameUtils.wildcardMatch(file.path, StringUtils.replace(exclusion, "/", File.separator), IOCase.INSENSITIVE)
            }
        }

        if (!excluded.empty) {
            project.logger.info "Excluding dependencies: ${excluded.collect { it.name }}"
        }

        def included = files - excluded
        def installables = included.findAll { file ->
            extension.fileInstallFilters.any { filter ->
                FilenameUtils.wildcardMatch(file.path, StringUtils.replace(filter, "/", File.separator), IOCase.INSENSITIVE)
            }
        }
        def deployables = included - installables

        if (!deployables.empty) {
            project.logger.info "Including framework dependencies: ${deployables.collect { it.name }}"
        }
        nonBundles += copyAndExcludeBundles(deployables, bundleDir)

        if (!installables.empty) {
            project.logger.info "Including installable dependencies : ${installables.collect { it.name }}"
        }
        nonBundles += copyAndExcludeBundles(installables, fileInstallDir)

        // TODO Wrap and decide where to copy (deploy dir or file install dir)
        if (!nonBundles.empty) {
            project.logger.info "Wrapping dependencies: ${nonBundles.collect { it.name }}"
            nonBundles.each { File file ->
                BundleWrapper.wrapNonBundle(file, bundleDir)
            }
        }
    }

    def Collection<File> copyAndExcludeBundles(Collection<File> files, String dir) {
        Collection<File> excluded = []
        project.copy {
            from files
            into dir
            exclude { FileTreeElement element ->
                def nonBundle = !BundleDetector.isBundle(element.file)
                if (nonBundle) {
                    excluded += element.file
                }

                return nonBundle
            }
        }

        return excluded
    }

    @Override
    def configs() {
        configFile.parentFile.mkdirs()
        configFile.write(configContent, FILE_ENCODING)
    }

    @Override
    def scripts() {
        def engine = new SimpleTemplateEngine()

        extension.runners.each { runner ->
            def target = new File("${extension.containerDir}/${runner.name}")
            def script = engine.createTemplate(runner).make([
                    'config' : extension,
                    'mainJar': mainJar
            ]).toString()
            target.write(script, FILE_ENCODING)
        }
    }

    String getConfigContent() {
        MapStringifier.asProperties(extension.config)
    }

    File getMainJar() {
        def config = project.configurations.getByName(ContainerConfig.MAIN)
        config.files.first()
    }

    String getBundleDir() {
        "${extension.containerDir}/${extension.bundlePath}"
    }

    String getFileInstallDir() {
        "${extension.containerDir}/${extension.fileInstallPath}"
    }

    File getConfigFile() {
        new File("${extension.containerDir}/${extension.configFile}")
    }

    ContainerExtension getExtension() {
        project.extensions.getByName(ContainerExtension.NAME) as ContainerExtension
    }

}