#!/usr/bin/env groovy

import br.com.uol.ps.pipelineutils.GithubAPI
import groovy.transform.Field

@Field NotificationUtils notificationUtils = new NotificationUtils()

def changeStatus(status) {
    def gitAPI = new GithubAPI(this)
    gitAPI.changeStatus(status, "pr_builder")
}

def notifyFailure(organization, projectName, buildUrl, buildNumber, prNumber, prTitle, assignee) {
    def pullRequestUrl = "https://github.com/${organization}/${projectName}/pull/"

    notificationUtils.sendMsg("""O build do PR falhou! Por favor, corrija o problema para que a PR seja revisada. O report do build pode ser acessado <${buildUrl}${buildNumber}|neste link.>
• <${pullRequestUrl}${prNumber}|${prTitle}>
• PR aberta por *${assignee}*
""", "error")
}
