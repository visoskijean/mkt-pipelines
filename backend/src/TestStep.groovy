#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = "test"

@Field testCommand = "./gradlew test --info"

@Field StepUtils stepUtils = new StepUtils()

def execute(evidencePathJunit, evidencePathCucumber) {
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
        junit evidencePathJUnit
    }
    if (evidencePathCucumber) {
        cucumber fileIncludePattern: evidencePathCucumber, sortingMethod: 'ALPHABETICAL'
    }
}

return this
