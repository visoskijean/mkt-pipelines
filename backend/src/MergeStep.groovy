#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = "merge"

@Field StepUtils stepUtils = new StepUtils()

@Field NotificationUtils notificationUtils = new NotificationUtils()

def execute(sourceBranch, targetBranch) {
    try {
        doExecute(sourceBranch, targetBranch)
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(sourceBranch, targetBranch) {
    stepUtils.merge(sourceBranch, targetBranch)
}
