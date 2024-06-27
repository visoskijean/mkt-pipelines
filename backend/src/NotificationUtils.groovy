#!/usr/bin/env groovy

import groovy.transform.Field
import java.util.regex.Pattern
import br.com.uol.ps.pipelineutils.TeamsAPI

@Field stepUtils

def sendMsg(text = '', color = 'info', fields = []) {
    commonFields = buildCommonFields()
    commonFields.addAll(fields)

    teamsAPI = new TeamsAPI(this)

	try {
		def messageObject = [
			'teams': 'iBanking',
			'channel': 'Deploy',
			'webhook_name': 'ibanking-deploy',
			'status': color,
			'title': "Ibanking Pipeline: Deploy em ${env.ENVIRONMENT.toUpperCase()} ([#${env.BUILD_NUMBER}](${env.BUILD_URL}))",
			'subtitle': text,
			'fields': commonFields.json()
		]

		echo messageObject.toString()

		teamsAPI.sendMessage(messageObject)
	} catch (err) {
		echo "Não foi possível enviar a mensagem para o canal do teams"
		echo err.message
	}
}

class FieldsToTeams {
	def list = []

	String add(title = '', value = '') {
		list.add("{\"name\": \"${title}\", \"value\": \"${value}\"}")
	}

    def addAll(elements) {
        list.addAll(elements)
    }

	String json() {
		return list.join(', ')
	}
}

FieldsToTeams buildCommonFields() {
    fields = new FieldsToTeams();

    def userName
    wrap([$class: 'BuildUser']) {
        userName = env.BUILD_USER
    }
    fields.add('Iniciado por:', userName)

    def projectName = stepUtils.getProjectName()
	fields.add("Aplicação:", projectName)

    def version = stepUtils.getVersion()
    fields.add("Versão:", version)

    def rfcYmlObj = readYaml file: './RFC.yml'
	fields.add("Coordenador da entrega (RFC.yml):", "${rfcYmlObj.coordinator}@pagseguro.com")

    return fields

}

def getStepUtils() {
    if (!this.@stepUtils) {
        this.@stepUtils = new StepUtils()
    }

    return this.@stepUtils
}
