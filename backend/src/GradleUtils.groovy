#!/usr/bin/env groovy

boolean containsTask(taskName) {
    def status = sh(script: "./gradlew -q tasks --all | grep -q ${taskName} && exit 0 || exit 1", returnStatus: true)
    return status == 0
}

def getPluginDependency(dependencyName) {
    def dependencies = sh(script: "./gradlew -q buildEnvironment | grep ${dependencyName} | awk '{print \$2}'", returnStdout: true).trim().split('\n')
    if (dependencies.size() == 0) {
        throw new Exception("Dependência ${dependencyName} não encontrada")
    }
    if (dependencies.size() > 1) {
        throw new Exception("Existem mais de uma dependência ${dependencyName} no seu projeto: ${dependencies}")
    }

    def dependency = dependencies[0].split(':')
    return [ groupId: dependency[0], artifactId: dependency[1], version: dependency[2] ]
}

return this