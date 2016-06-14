package com.neva.gradle.plugin.osgi.container.task

import com.neva.gradle.plugin.osgi.container.ContainerExtension
import org.gradle.api.internal.AbstractTask

class ContainerTask extends AbstractTask {

    def ContainerExtension getExtension() {
        project.extensions.getByName(ContainerExtension.NAME) as ContainerExtension
    }

}