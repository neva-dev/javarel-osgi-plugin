package com.neva.gradle.plugin.osgi.container.builder

import org.gradle.api.Project

class FelixBuilder extends AbstractBuilder {

    FelixBuilder(Project project) {
        super(project)
    }

    @Override
    def init() {
        extension.exclude([
                'org.apache.felix.main*',
                'org.apache.felix.scr.annotations*',
        ])
        extension.mainDependency = "org.apache.felix.main-*"
        extension.bundlePath = 'bundle'
        extension.runners += project.file("osgiContainer/felix/run.sh")
        extension.config(project.file("osgiContainer/felix/config.properties"))
    }
}
