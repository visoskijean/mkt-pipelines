pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Passo de checkout do repositório'
            }
        }
        stage('Build') {
            steps {
                echo 'Passo de build da aplicação'
            }
        }
        stage('Test') {
            steps {
                echo 'Passo de teste da aplicação'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Passo de deploy da aplicação'
            }
        }
    }
}
