#!/usr/bin/env groovy

import groovy.transform.Field

@Field TASK_NAME = "sonarqube"

@Field SONAR_ENVIRONMENT = "sonarqubeReleaseProd"

@Field int SONAR_QUALITY_GATE_TIMEOUT_IN_MINUTES = 5

@Field int SONAR_RETRY_TIMES = 2

@Field GradleUtils gradleUtils = new GradleUtils()

@Field NotificationUtils notificationUtils = new NotificationUtils()

def runBranchAnalysis(projectName, branchName) {
    if (!gradleUtils.containsTask(TASK_NAME)) {
        print("Projeto ${projectName} não contém task ${TASK_NAME}")
        return
    }

    retry (SONAR_RETRY_TIMES) {
        withSonarQubeEnv(SONAR_ENVIRONMENT) {
            sh "./gradlew ${TASK_NAME} -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.branch.name=${branchName} --info"
        }
        waitForSonarQualityGate(projectName)
    }
}

def runPullRequestAnalysis(projectName, key, branch, baseBranch) {
    if (!gradleUtils.containsTask(TASK_NAME)) {
        print("Projeto ${projectName} não contém task ${TASK_NAME}")
        return
    }
    withSonarQubeEnv(SONAR_ENVIRONMENT) {
        sh """./gradlew ${TASK_NAME} -Dsonar.host.url=${SONAR_HOST_URL} \
            -Dsonar.pullrequest.key=${key} \
            -Dsonar.pullrequest.branch=${branch} \
            -Dsonar.pullrequest.base=${baseBranch} --info
        """
    }
    waitForSonarQualityGate(projectName)
}

def waitForSonarQualityGate(projectName) {
    def waitForQualityGateProjects = getWaitForQualityGateProjects()

    echo "Projetos configurados para aguardar o Quality Gate: ${waitForQualityGateProjects}"

    def project = projectName.trim().toLowerCase()

    if (project in waitForQualityGateProjects) {
        timeout(time: SONAR_QUALITY_GATE_TIMEOUT_IN_MINUTES, unit: 'MINUTES') {
            def qg = waitForQualityGate()
            if (qg.status != 'OK') {
                throw new Exception("Erro no Quality Gate do SonarQube para o projeto ${projectName}. Status: ${qg.status}")
            }
        }
    }
}

def getWaitForQualityGateProjects() {
    if (!env.WAIT_FOR_QUALITY_GATE_PROJECTS) {
        return []
    }
    return env.WAIT_FOR_QUALITY_GATE_PROJECTS.split(",").collect { it.trim().toLowerCase() }
}

return this
