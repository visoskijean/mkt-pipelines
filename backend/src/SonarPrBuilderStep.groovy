#!/usr/bin/env groovy

import groovy.transform.Field

@Field STEP_NAME = "sonar"

@Field StepUtils stepUtils = new StepUtils()

@Field SonarUtils sonarUtils = new SonarUtils()

def execute(projectName, key, branch, baseBranch) {
    try {
        doExecute(projectName, key, branch, baseBranch)
    } catch (err) {
        stepUtils.stepFailure(STEP_NAME, err)
    }
}

def doExecute(projectName, key, branch, baseBranch) {
    sonarUtils.runPullRequestAnalysis(projectName, key, branch, baseBranch)
}

return this