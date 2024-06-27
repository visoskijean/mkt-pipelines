#!/usr/bin/env groovy

import groovy.transform.Field
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

@Field stepName = "fortify"

@Field StepUtils stepUtils = new StepUtils()

@Field FORTIFY_URL = 'https://fortify-ssc.intranet.pags/ssc/html/ssc/version'

def execute(organization, repositoryName, branch) {
    try {
        doExecute(organization, repositoryName.trim(), branch, true)
    } catch (FlowInterruptedException err) {
        stepUtils.stepFailure(stepName, new Exception("Verifique o resultado da análise do Fortify em: ${FORTIFY_URL}"))
    } catch (Exception err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(organization, repositoryName, branch, wait) {
    build(
        job: 'COMMONS/fortify',
        wait: wait,
        parameters: [
            string(name: 'PROJECT_KEY',             value: "${organization}"),
            string(name: 'REPOSITORY',              value: "${repositoryName}"),
            string(name: 'BRANCH',                  value: "${branch}"),
            booleanParam(name: 'BLOCK_CRITICAL',    value: true),
            booleanParam(name: 'BLOCK_HIGH',        value: false),
            booleanParam(name: 'BLOCK_MEDIUM',      value: false),
            booleanParam(name: 'BLOCK_LOW',         value: false),
            booleanParam(name: 'MIRROR_ANALYSIS',   value: false),
            booleanParam(name: 'IGNORE_REPORTED',   value: false)
        ],
    )
}

return this