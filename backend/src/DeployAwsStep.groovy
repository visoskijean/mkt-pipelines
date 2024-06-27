#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = 'deployAws'

@Field StepUtils stepUtils = new StepUtils()

@Field NotificationUtils notificationUtils = new NotificationUtils()

def execute(organization, repositoryName, branch, environment, version) {
    try {
        doExecute(organization, repositoryName.trim(), branch, environment, version)
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(organization, repositoryName, branch, environment, version) {
    def dockerRegistry = stepUtils.getDockerRegistry()

    def dockerRepository = stepUtils.getDockerImageRepository()

    def imageName = stepUtils.getDockerImageName()
    def containerImage = stepUtils.formatContainerImage(dockerRegistry, dockerRepository, imageName, version)

    print("${stepName}: environment: ${environment}, imageName: ${imageName}, version: ${version}, branch: ${branch}")

    echo """
    job: COMMONS/ecs-service-deploy
    parameters:
        PROJECT_KEY=${organization}
        REPOSITORY=${repositoryName}
        BRANCH=${branch}
        ENVIRONMENT=${environment}
        TAG=${version}
        CONTAINER_IMAGE=${containerImage}
        SCM=GITHUB
        TERRAFORM_ACTION=apply
        CREDENTIALS=svcacc_apis
    """

    build(
        job: "COMMONS/ecs-service-deploy",
        wait: true,
        parameters: [
            [$class: "StringParameterValue", name: "PROJECT_KEY",       value: "${organization}"],
            [$class: "StringParameterValue", name: "REPOSITORY",        value: "${repositoryName}"],
            [$class: "StringParameterValue", name: "BRANCH",            value: "${branch}"],
            [$class: "StringParameterValue", name: "ENVIRONMENT",       value: "${environment}"],
            [$class: "StringParameterValue", name: "TAG",               value: "${version}"],
            [$class: "StringParameterValue", name: "CONTAINER_IMAGE",   value: "${containerImage}"],
            [$class: "StringParameterValue", name: "SCM",               value: "GITHUB"],
            [$class: "StringParameterValue", name: "TERRAFORM_ACTION",  value: "apply"],
            [$class: "StringParameterValue", name: "CREDENTIALS",       value: "svcacc_apis"]
        ],
        quietPeriod: 3
    )
}

return this
