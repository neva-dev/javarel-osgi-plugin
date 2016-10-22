package com.neva.gradle.plugin.osgi.container.util

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.apache.commons.lang3.StringUtils

final class FileFilter {

    static wildcardJarPattern(String pattern) {
        if (pattern.endsWith("*") || pattern.endsWith(".jar")) {
            return "*/$pattern"
        } else {
            return "*/$pattern/*"
        }
    }

    static wildcardJarFile(File file, String pattern) {
        FilenameUtils.wildcardMatch(file.path, StringUtils.replace(wildcardJarPattern(pattern), "/", File.separator), IOCase.INSENSITIVE)
    }

}
