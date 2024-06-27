#!/usr/bin/env groovy

import groovy.transform.Field
import br.com.uol.ps.pipelineutils.KubernetesHelper

@Field stepName = "deployPagCloud"

@Field StepUtils stepUtils = new StepUtils()

@Field NotificationUtils notificationUtils = new NotificationUtils()

def execute(environment, version) {
    try {
        doExecute(environment, version)
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

def doExecute(environment, version) {
    def deployDir = "deploy/${environment}"

    def dockerRegistry = stepUtils.getDockerRegistry()
    def dockerRepository = stepUtils.getDockerImageRepository()

    def imageName = stepUtils.getDockerImageName()
    def containerImage = stepUtils.formatContainerImageEscaped(dockerRegistry, dockerRepository, imageName, version)

    print("${stepName}: environment: ${environment}, imageName: ${imageName}, version: ${version}")

    stepUtils.createDirectory(deployDir)
    stepUtils.copyFile("RFC.yml", deployDir)

    generateDeploymentConfig(environment, "gt", containerImage, deployDir)
    generateDeploymentConfig(environment, "tb", containerImage, deployDir)

    def kubernetesHelper = new KubernetesHelper(this)
    kubernetesHelper.deployTo(environment)

    stepUtils.removeDirectory("deploy")
}

def generateDeploymentConfig(environment, site, containerImage, deployDir) {
    echo "${stepName}: Gerando configuração de deployment para ${environment} ${site}"

    def templateFile = "k8s/${environment}/${environment}.yaml"
    def targetFile = "${deployDir}/${site}.yaml"

    def environmentUppercase = environment.toUpperCase()
    def siteUppercase = site.toUpperCase()

    sh("""sed -e "s/\\\${ENVIRONMENT}/${environmentUppercase}/" \\
              -e "s/\\\${SITE}/${siteUppercase}/" \\
              -e "s/\\\${ENVIRONMENT_L}/${environment}/" \\
              -e "s/\\\${SITE_L}/${site}/" \\
              -e "s/\\\${CONTAINER_IMAGE}/${containerImage}/" \\
              ${templateFile} > ${targetFile}
        """)

    sh "cat ${targetFile}"
}

return this
