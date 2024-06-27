#!/usr/bin/env groovy

import groovy.transform.Field

@Field javaVersions = [
    'Java 21': 'java21',
    'Java 17': 'java17',
    'Java 15': 'java15',
    'Java 11': 'java11',
]

@Field organizations = [ 'ps-web-channel' ]

@Field stepUtils

@Field notificationUtils

fields = new Fields()

deployedPlatforms = []

requestedDBMigrationStep = false

properties([
    parameters([
        choice(name: 'ORGANIZATION', choices: organizations, description: 'Organization do Github'),
        string(name: 'REPOSITORY_NAME', defaultValue: '', description: 'Apenas o nome do repositório, sem host e protocolo.'),
        ParamUtils.branchSelectionParameter(name: 'BRANCH', dependsOn: 'ORGANIZATION, REPOSITORY_NAME', description: 'Selecione o branch da entrega após definir o repositório.'),
        choice(name: 'JAVA_VERSION', choices: javaVersions.keySet() as List, description: 'Versão do Java utilizado na aplicação'),
        booleanParam(name: 'RUN_DB_MIGRATION', defaultValue: false, description: 'Executa step de migração de base.'),
        booleanParam(name: 'RUN_DB_REPAIR', defaultValue: false, description: '(opcional) Executa step de reparo de migração de base. Depende do step de migração de base estar habilitado.')
    ])
])

pipeline {
    agent {
        label javaVersions[params.JAVA_VERSION]
    }
    options {
        skipDefaultCheckout true
    }
    environment {
        JAVA_VERSION = "${javaVersions[params.JAVA_VERSION]}"
        JAVA_HOME = "${tool env.JAVA_VERSION}"
        PATH = "${env.JAVA_TOOL}/bin:${env.PATH}"
        EVIDENCE_PATH_JUNIT_TEST = "**/build/test-results/test/TEST-*.xml"
        EVIDENCE_PATH_JUNIT_INTEGRATION_TEST = "**/build/test-results/integrationTest/TEST-*.xml"
        EVIDENCE_PATH_CUCUMBER = "**/build/*.json"
        ENVIRONMENT = "dev"
        NOTIFICATION_MSG_HEADER = "JOB ${currentBuild.number} - [${params.REPOSITORY_NAME}][${env.ENVIRONMENT.toUpperCase()}]"
        DOCKER_REGISTRY_URL = "https://repo.intranet.pags"
        NEW_RELIC_ACCOUNT = "nr-pagseguro-${ENVIRONMENT}"
    }
    stages {
        stage ('Clone') {
            steps {
                script {
                    def cloneStep = new CloneStep()
                    cloneStep.execute(params.ORGANIZATION, params.REPOSITORY_NAME, params.BRANCH, null)
                    stepUtils.notifyDeployStarted()
                }
            }
        }
        stage ('Build') {
            steps {
                script {
                    def buildStep = new BuildStep()
                    buildStep.execute()
                }
            }
        }
        stage ('Test') {
            steps {
                script {
                    def testStep = new TestStep()
                    testStep.execute(env.EVIDENCE_PATH_JUNIT_TEST, env.EVIDENCE_PATH_CUCUMBER)
                }
            }
        }
        stage ('Integration Test') {
            steps {
                script {
                    def integrationTestStep = new IntegrationTestStep()
                    integrationTestStep.execute(env.EVIDENCE_PATH_JUNIT_INTEGRATION_TEST, env.EVIDENCE_PATH_CUCUMBER)
                }
            }
        }
        stage ('DB Migration') {
            when { expression { return params.RUN_DB_MIGRATION } }
            steps {
                script {
                    def dbMigrationStep = new DbMigrationStep()
                    dbMigrationStep.migrate(env.ENVIRONMENT, params.RUN_DB_REPAIR)
                    requestedDBMigrationStep = true
                }
            }
        }
        stage ('Release') {
            steps {
                script {
                    def releaseStep = new ReleaseStep()
                    releaseStep.execute(env.DOCKER_REGISTRY_URL, null, null)
                }
            }
        }
        stage ('Deploy AWS') {
            when { expression { script { return stepUtils.isAwsDeploy() } } }
            steps {
                script {
                    deployedPlatforms.add('ECS')
                    def version = stepUtils.formatDevelopmentTag()
                    def deployAwsStep = new DeployAwsStep()
                    deployAwsStep.execute(params.ORGANIZATION, params.REPOSITORY_NAME, params.BRANCH, env.ENVIRONMENT, version)
                }
            }
        }
        stage ('Deploy PagCloud') {
            when { expression { script { return stepUtils.isPagCloudDeploy() } } }
            steps {
                script {
                    deployedPlatforms.add('K8S')
                    def version = stepUtils.formatDevelopmentTag()
                    def deployPagCloudStep = new DeployPagCloudStep()
                    deployPagCloudStep.execute(env.ENVIRONMENT, version)
                }
            }
        }
        stage ('Cleanup') {
            steps {
                script {
                    def version = stepUtils.formatDevelopmentTag()
                    stepUtils.sendNewRelicDeployNotification(env.NEW_RELIC_ACCOUNT, version)

                    addCommonFields()
                    notificationUtils.sendMsg("O ambiente de ${env.ENVIRONMENT.toUpperCase()} foi atualizado com sucesso!", "success", fields.list)
                }
            }
        }
    }
}

def getStepUtils() {
    if (!this.@stepUtils) {
        this.@stepUtils = new StepUtils()
    }

    return this.@stepUtils
}

def getNotificationUtils() {
    if (!this.@notificationUtils) {
        this.@notificationUtils = new NotificationUtils()
    }

    return this.@notificationUtils
}

def addStepFields() {

}

def addCommonFields() {
    def deployedPlatformsStr = deployedPlatforms.join(', ')
    fields.add("Plataforma:", deployedPlatformsStr)

    def fieldDBMigration = requestedDBMigrationStep ? "Executado" : "Não executado"
    fields.add("DBMigration:", fieldDBMigration)
}

class Fields {
	def list = []

	String add(title = '', value = '') {
		list.add("{\"name\": \"${title}\", \"value\": \"${value}\"}")
	}
}
