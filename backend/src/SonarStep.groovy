#!/usr/bin/env groovy

import groovy.transform.Field

@Field STEP_NAME = "sonar"

@Field StepUtils stepUtils = new StepUtils()

@Field SonarUtils sonarUtils = new SonarUtils()

def execute(projectName, branchName) {
    try {
        doExecute(projectName, branchName)
    } catch (err) {
        stepUtils.stepFailure(STEP_NAME, err)
    }    
}

def doExecute(projectName, branchName) {
    sonarUtils.runBranchAnalysis(projectName, branchName)
}

return this