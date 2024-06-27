#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = "release"

@Field releaseCommand = "./gradlew releaseDockerImage"

@Field StepUtils stepUtils = new StepUtils()

@Field NotificationUtils notificationUtils = new NotificationUtils()

def execute(dockerRegistryUrl, versionStrategy, branch) {
    try {
        doExecute(dockerRegistryUrl, versionStrategy, branch)
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(dockerRegistryUrl, versionStrategy, branch) {
    def version = changeVersion(versionStrategy)

    buildDistArchive()

    releaseDockerImage(dockerRegistryUrl, version)

    commitNewVersion(version, branch)

    createGitTag(version)

    updateBuildDisplayName(version)
}

def changeVersion(versionStrategy) {
    if (versionStrategy) {
        sh "./gradlew changeVersion -DversionStrategy=${versionStrategy}"
        return stepUtils.getVersion()
    }

    return stepUtils.formatDevelopmentTag()
}

def buildDistArchive() {
    sh "./gradlew bootDistZip"
}

def releaseDockerImage(dockerRegistryUrl, version) {
    doReleaseDockerImage(dockerRegistryUrl, version)
}

def doReleaseDockerImage(dockerRegistryUrl, version) {
    // TODO: permitir criar latest
    docker.withRegistry(dockerRegistryUrl, 'svcacc_ps_jenkins') {
        sh "${releaseCommand} -PdockerImageTag=${version}"
    }
}

def commitNewVersion(version, branch) {
    if (stepUtils.isDevelopmentTag(version)) {
        return
    }

    stepUtils.commit(branch, "Release $version")
}

def createGitTag(version) {
    if (stepUtils.isDevelopmentTag(version)) {
        return
    }

    def tag = stepUtils.formatReleaseCandidateTag()
    stepUtils.createGitTag(tag)
}

def updateBuildDisplayName(version) {
    def projectName = stepUtils.getProjectName()
    currentBuild.displayName = "#${currentBuild.number} ${projectName} [${version}]"
}

return this
