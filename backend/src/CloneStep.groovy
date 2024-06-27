#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = 'clone'

@Field StepUtils stepUtils = new StepUtils()

def execute(organization, repositoryName, branchOrTag, versionStrategy) {
    if (!repositoryName) {
        error("Invalid repository name")
    }
    try {
        doExecute(organization, repositoryName.trim(), branchOrTag, versionStrategy)
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(organization, repositoryName, branchOrTag, versionStrategy) {
    deleteDir()

    cloneProject(organization, repositoryName, branchOrTag)

    updateBuildInfo(versionStrategy, branchOrTag)
}

def cloneProject(organization, repositoryName, branchOrTag) {
    checkout scm: [
        $class: 'GitSCM',
        userRemoteConfigs: [[url: "git@github.com:${organization}/${repositoryName}.git", credentialsId: 'svcacc_ps_jenkins_ssh']],
        branches: [[name: branchOrTag]]]
}

def updateBuildInfo(versionStrategy, branchOrTag) {
    def projectName = stepUtils.getProjectName()

    currentBuild.displayName = "#${currentBuild.number} ${projectName}"
    currentBuild.description = "Build from ${projectName}. Target branch \"${branchOrTag}\"."
    if (versionStrategy) {
        currentBuild.description = "${currentBuild.description} Version startegy is ${versionStrategy}"
    } else {
        def version = stepUtils.getVersion()
        currentBuild.displayName = "${currentBuild.displayName} [$version]"
    }

}

return this
