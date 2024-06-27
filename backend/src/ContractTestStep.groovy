#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = "contractTest"

@Field testCommand = "./gradlew contractTest -x test"

@Field StepUtils stepUtils = new StepUtils()

@Field GradleUtils gradleUtils = new GradleUtils()

def execute(evidencePathJunit, evidencePathCucumber) {
    if (!gradleUtils.containsTask(stepName)) {
        print("Projeto não contém task ${stepName}")
        return
    }

    try {
        doExecute()
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    } finally {
        collectTestEvidence(evidencePathJunit, evidencePathCucumber)
    }
}

def doExecute() {
    sh "${testCommand}"
}

def collectTestEvidence(evidencePathJUnit, evidencePathCucumber) {
    if (evidencePathJUnit) {
        junit testResults: evidencePathJUnit
    }
    if (evidencePathCucumber) {
        cucumber fileIncludePattern: evidencePathCucumber, sortingMethod: 'ALPHABETICAL'
    }
}

return this
