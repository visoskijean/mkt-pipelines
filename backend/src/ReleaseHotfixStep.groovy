#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = "release hotfix"

@Field StepUtils stepUtils = new StepUtils()

def execute(dockerRegistryUrl, versionStrategy, branch) {
    try {
        doExecute(dockerRegistryUrl, versionStrategy, branch)
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(dockerRegistryUrl, versionStrategy, branch) {
    def latestVersion = stepUtils.findLatestVersionFromCurrentSeries()
    stepUtils.setVersion(latestVersion)

    def releaseStep = new ReleaseStep()
    releaseStep.execute(dockerRegistryUrl, versionStrategy, branch)
}
