#!/usr/bin/env groovy

import groovy.transform.Field

@Field GradleUtils gradleUtils = new GradleUtils()

@Field stepName = "dbMigrationStep"

def migrate(String envValue, boolean executeRepair) {
    def dependencyVersion = getPluginDependencyVersion("database-migration-plugin:database-migration-plugin")

    def config = getConfig(dependencyVersion)

    if (executeRepair) {
        executeTask(config.databaseRepairTask, config.envProperty, envValue)
    }

    executeTask(config.databaseMigrationTask, config.envProperty, envValue)

    print("${stepName}: Migração de base executada!")
}

def getPluginDependencyVersion(String dependencyName) {
    return gradleUtils.getPluginDependency(dependencyName).version
}

def getConfig(String version) {
    def parts = version.split('\\.')
    def major = Integer.parseInt(parts[0])
    def minor = Integer.parseInt(parts[1])
    def patch = Integer.parseInt(parts[2])

    if (!isValidVersion(major, minor, patch)) {
        def message = "${stepName} suporta database-migration-plugin:database-migration-plugin 1.0.1 ou superior"
        print(message)
        throw new Exception(message)
    }

    return [
        databaseRepairTask: "databaseRepair",
        databaseMigrationTask: "databaseMigrate",
        envProperty: 'env'
    ]
}

def isValidVersion(major, minor, patch) {
    if (major == 0) {
        return false
    }
    if (major == 1 && minor == 0 && patch == 0) {
        return false
    }
    return true
}

def executeTask(String taskName, String envProperty, String envValue) {
    if (!gradleUtils.containsTask(taskName)) {
        throw new Exception("Projeto não contém task ${taskName}")
    }

    sh "./gradlew ${taskName} -D${envProperty}=${envValue} --stacktrace --info"
}

return this