#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = "merge hotfix develop"

@Field developBranch = "develop"

@Field StepUtils stepUtils = new StepUtils()

@Field NotificationUtils notificationUtils = new NotificationUtils()

def execute(branch) {
    try {
        doExecute(branch)
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(branch) {
    // Commit develop version into hotfix branch to avoid conflicting gradle.properties file when merging
    def developVersion = findVersionFrom(developBranch)
    stepUtils.setVersion(developVersion)
    stepUtils.commit(branch, "Prepare hotfix branch for merging into develop")
    stepUtils.merge(branch, developBranch)

    stepUtils.checkout(developBranch)

    // Write latest version from current series so when releasing a new RC, it will use the next available version
    def latestVersionSeries = stepUtils.findLatestVersionFromCurrentSeries()
    stepUtils.setVersion(latestVersionSeries)
    stepUtils.commit(developBranch, "Prepare develop branch for creating RC with hotfix")

    stepUtils.checkout(branch)
}

def findVersionFrom(targetBranch) {
    checkoutPropertiesFileFrom(targetBranch)
    def version = stepUtils.getVersion()
    return version
}

def checkoutPropertiesFileFrom(branch) {
    try {
        sh "git fetch"
        sh "git checkout origin/${branch} -- ${stepUtils.propertiesFileName}"
    } catch(e) {
        error("Falha ao realizar checkout do ${stepUtils.propertiesFileName} a partir do branch ${branch}. Erro: ${e.message}")
    }
}

return this
