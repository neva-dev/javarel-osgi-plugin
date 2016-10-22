package com.neva.gradle.plugin.osgi.container.builder

interface ContainerBuilder {

    def configure()

    def init()

    def main()

    def bundles()

    def configs()

    def scripts()

}