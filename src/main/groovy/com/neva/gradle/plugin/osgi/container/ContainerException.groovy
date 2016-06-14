package com.neva.gradle.plugin.osgi.container

import org.gradle.api.GradleException

class ContainerException extends GradleException {

    ContainerException(String message) {
        super(message)
    }

}
