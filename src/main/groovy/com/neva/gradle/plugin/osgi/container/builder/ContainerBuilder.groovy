package com.neva.gradle.plugin.osgi.container.builder

interface ContainerBuilder {

    def init()

    def main()

    def bundles()

    def configs()

    def scripts()

}