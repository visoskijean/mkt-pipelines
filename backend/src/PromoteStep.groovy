#!/usr/bin/env groovy

import groovy.transform.Field
import br.com.uol.ps.pipelineutils.JfrogHelper

@Field stepName = "promote"

@Field StepUtils stepUtils = new StepUtils()

@Field NotificationUtils notificationUtils = new NotificationUtils()

def execute(dockerRegistryUrl) {
    try {
        doExecute(dockerRegistryUrl)
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(dockerRegistryUrl) {
    def version = stepUtils.getVersion()
    def imageName = stepUtils.getDockerImageName()

    promote(imageName, version, dockerRegistryUrl)

    createPromotedReleaseTag()

    updateBuildInfo(imageName, version)
}

def promote(imageName, version, dockerRegistryUrl) {
    docker.withRegistry(dockerRegistryUrl, 'svcacc_ps_jenkins') {
        JfrogHelper jfrogHelper = new JfrogHelper(this)
        jfrogHelper.promote(version, imageName)
    }
}

def createPromotedReleaseTag() {
    def tag = stepUtils.formatPromotedReleaseTag()
    stepUtils.createGitTag(tag)
}

def updateBuildInfo(imageName, version) {
    currentBuild.description = "Build from ${imageName}. Version to deploy \"${version}\"."
}

return this
