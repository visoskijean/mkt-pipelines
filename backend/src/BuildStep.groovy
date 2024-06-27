#!/usr/bin/env groovy

import groovy.transform.Field

@Field stepName = 'build'

@Field StepUtils stepUtils = new StepUtils()

/**
 * Compila o projeto.
 * Não gera os artefatos de distribuição por não serem utilizados e para não conflitar com os artefatos que são gerados
 * no step de release (que utiliza a versão do projeto atualizada).
 */
def execute() {
    try {
        sh "./gradlew clean build -x test -x distZip -x distTar -x bootDistTar -x bootDistZip"
    } catch (err) {
        stepUtils.stepFailure(stepName, err)
    }
}

return this
