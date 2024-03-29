package com.neva.gradle.plugin.osgi.container

import com.neva.gradle.plugin.osgi.container.builder.ContainerBuilder
import com.neva.gradle.plugin.osgi.container.builder.DefaultBuilder

import com.neva.gradle.plugin.osgi.container.util.FileFilter
import org.gradle.api.Project

class ContainerExtension {

    static final NAME = 'osgiContainer'

    Project project

    ContainerBuilder builder

    Map<String, Object> config = [:]

    String mainDependency

    List<File> runners = []

    String bundlePath = 'bundle'

    String configFile = 'conf/config.properties'

    String containerDir = "build/osgiContainer"

    String javaCommand = 'java'

    List<String> javaArgs = []

    List<String> programArgs = []

    List<String> exclusions = []

    List<String> fileInstallFilters = []

    String fileInstallPath = 'load'

    ContainerExtension(Project project) {
        this.project = project
        this.builder = new DefaultBuilder(project, this)
    }

    def config(File file) {
        def props = new Properties()
        props.load(new FileInputStream(file))
        config.putAll(props as Map)
    }

    def config(String key, Object value) {
        config.put(key, value)
    }

    def debug(Integer port = 16660, Boolean suspend = false) {
        javaArgs << "-agentlib:jdwp=transport=dt_socket,server=y,suspend=${suspend ? 'y' : 'n'},address=$port".toString()
    }

    def javaArg(String arg) {
        javaArgs += arg
    }

    String getJavaArgs() {
        return javaArgs.join(' ')
    }

    def programArg(String arg) {
        programArgs += arg
    }

    String getProgramArgs() {
        return programArgs.join(' ')
    }

    def getContainerDirAbsolute() {
        return new File(containerDir).absolutePath
    }

    def exclude(List<String> exclusions) {
        exclusions.each {
            exclude(it)
        }
    }

    def exclude(String exclusion) {
        addFilter(exclusions, exclusion)
    }

    def exclude(String group, String name, String version = null) {
        String exclusion = "$group/$name"
        if (version != null) {
            exclusion += "/*/$name-$version*"
        }

        exclude(exclusion)
    }

    def later(String pattern) {
        addFilter(fileInstallFilters, pattern)
    }

    def later(List<String> patterns) {
        patterns.each {
            later(it)
        }
    }

    private addFilter(List<String> filters, String exclusion) {
        filters.add(FileFilter.wildcardJarPattern(exclusion))
    }

}
