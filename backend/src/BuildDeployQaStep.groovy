#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = 'build deploy qa'

@Field StepUtils stepUtils = new StepUtils()

@Field NotificationUtils notificationUtils = new NotificationUtils()

def execute(versionStrategy, organization, repositoryName, javaVersionLabel, runDbMigration, runDbRepair) {
    try {
        doExecute(versionStrategy, organization, repositoryName.trim(), javaVersionLabel, runDbMigration, runDbRepair)
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(versionStrategy, organization, repositoryName, javaVersionLabel, runDbMigration, runDbRepair) {

    result = build(job: "CUSTOMER_SUCCESS/ibanking-services/IBANKING/backend/deploy-service-qa",
        parameters: [
            [
                $class: "StringParameterValue",
                name  : "ORGANIZATION",
                value : organization
            ],
            [
                $class: "StringParameterValue",
                name  : "VERSION_STRATEGY",
                value : versionStrategy
            ],
            [
                $class: "StringParameterValue",
                name  : "REPOSITORY_NAME",
                value : repositoryName
            ],
            [
                $class: "StringParameterValue",
                name  : "JAVA_VERSION",
                value : javaVersionLabel
            ],
            [
                $class: "BooleanParameterValue",
                name  : "RUN_DB_MIGRATION",
                value : runDbMigration
            ],
            [
                $class: "BooleanParameterValue",
                name  : "RUN_DB_REPAIR",
                value : runDbRepair
            ]
        ],
        propagate: false
    ).result

    if (result == 'FAILURE') {
        error("Falha executar o pipeline de deploy-service-qa")
    }
}
