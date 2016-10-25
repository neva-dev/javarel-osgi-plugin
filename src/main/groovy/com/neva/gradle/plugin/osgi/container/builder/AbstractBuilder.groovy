package com.neva.gradle.plugin.osgi.container.builder

import com.neva.gradle.plugin.osgi.container.ContainerConfig
import com.neva.gradle.plugin.osgi.container.ContainerException
import com.neva.gradle.plugin.osgi.container.ContainerExtension
import com.neva.gradle.plugin.osgi.container.util.*
import groovy.text.SimpleTemplateEngine
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement

abstract class AbstractBuilder implements ContainerBuilder {

    static final FILE_ENCODING = 'UTF-8'

    Project project

    Collection<File> jars

    AbstractBuilder(Project project) {
        this.project = project

        configure()

        extension.exclude(["junit*", extension.mainDependency])
    }

    @Override
    def init() {
        this.jars = DependencyResolver.jars(project)
    }

    @Override
    def main() {
        project.copy {
            from mainJar
            into extension.containerDir
        }
    }

    @Override
    def bundles() {
        def nonBundles = []

        project.logger.info "Exclusion filters: ${extension.exclusions}"

        def excluded = jars.findAll { file ->
            extension.exclusions.any { exclusion -> FileFilter.wildcardJarFile(file, exclusion) }
        }

        if (!excluded.empty) {
            project.logger.info "Excluding dependencies: ${excluded.collect { it.name }}"
        }

        project.logger.info "File install filters: ${extension.fileInstallFilters}"

        def included = jars - excluded
        def installables = included.findAll { file ->
            extension.fileInstallFilters.any { filter -> FileFilter.wildcardJarFile(file, filter) }
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
        def jar = null

        def configFiles = project.configurations.getByName(ContainerConfig.MAIN).files
        if (!configFiles.empty) {
            jar = configFiles.first()
            if (configFiles.size() > 1) {
                project.logger.warn "Configuration named '${ContainerConfig.MAIN}' should not have more than one dependency defined."
            }
        }

        if (jar == null) {
            def filterFiles = jars.findAll { file -> FileFilter.wildcardJarFile(file, extension.mainDependency) }
            if (!filterFiles.empty) {
                jar = filterFiles.first()
                if (filterFiles.size() > 1) {
                    project.logger.warn "Found more than one container main dependency '${filterFiles}', first used."
                }
            }
        }

        if (jar == null) {
            throw new ContainerException("Main OSGi container jar not found. Please add valid 'osgiMain' dependency"
                    + " or verify 'mainDependency' property of 'osgiContainer' extension.")
        }

        return jar
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
