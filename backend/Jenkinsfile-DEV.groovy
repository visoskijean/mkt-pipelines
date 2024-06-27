#!/usr/bin/env groovy

@Field javaVersions = [
    'Java 21': 'java21',
    'Java 17': 'java17',
    'Java 15': 'java15',
    'Java 11': 'java11',
]

@Field organizations = [ 'ps-web-channel' ]

properties([
    parameters([
        choice(name: 'ORGANIZATION', choices: organizations, description: 'Organization do Github'),
        string(name: 'REPOSITORY_NAME', defaultValue: '', description: 'Apenas o nome do repositório, sem host e protocolo.'),
        string(name: 'BRANCH', defaultValue: 'main', description: 'Branch da entrega após definir o repositório.'),
        choice(name: 'JAVA_VERSION', choices: javaVersions.keySet() as List, description: 'Versão do Java utilizado na aplicação'),
        booleanParam(name: 'RUN_DB_MIGRATION', defaultValue: false, description: 'Executa step de migração de base.'),
        booleanParam(name: 'RUN_DB_REPAIR', defaultValue: false, description: '(opcional) Executa step de reparo de migração de base. Depende do step de migração de base estar habilitado.')
    ])
])

pipeline {
    agent any
    stages {
        stage ('Clone') {
            steps {
                echo "Clone Step - Organization: ${params.ORGANIZATION}, Repository: ${params.REPOSITORY_NAME}, Branch: ${params.BRANCH}"
            }
        }
        stage ('Build') {
            steps {
                echo "Build Step - Java Version: ${params.JAVA_VERSION}"
            }
        }
        stage ('Test') {
            steps {
                echo "Test Step"
            }
        }
        stage ('Integration Test') {
            steps {
                echo "Integration Test Step"
            }
        }
        stage ('DB Migration') {
            when { expression { return params.RUN_DB_MIGRATION } }
            steps {
                echo "DB Migration Step - Run Repair: ${params.RUN_DB_REPAIR}"
            }
        }
        stage ('Release') {
            steps {
                echo "Release Step"
            }
        }
        stage ('Deploy AWS') {
            steps {
                echo "Deploy AWS Step"
            }
        }
        stage ('Deploy PagCloud') {
            steps {
                echo "Deploy PagCloud Step"
            }
        }
        stage ('Cleanup') {
            steps {
                echo "Cleanup Step"
            }
        }
    }
}
