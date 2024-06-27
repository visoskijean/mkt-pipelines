#!/usr/bin/env groovy

import groovy.transform.Field
import java.util.regex.Pattern

@Field propertiesFileName = "gradle.properties"

@Field NotificationUtils notificationUtils = new NotificationUtils()

def stepFailure(String stepName, Exception err) {
    def reason = formatFailureReason(err)
    String errorMessage = ":x: O step de $stepName falhou!\n${reason}"
    notificationUtils.sendMsg(errorMessage, "error")
    throw err
}

static def formatFailureReason(Exception err) {
    def msg = err.message ?: err.toString()
    if (msg.contains("FlowInterruptedException")) {
        return "O deploy foi ativamente interrompido! Ex.: interrupção manual ou cancelamento de RFC"
    }

    return msg
}

static def formatContainerImage(dockerRegistry, dockerRepository, projectName, version) {
    return "${dockerRegistry}/${dockerRepository}/${projectName}:${version}"
}

static def formatContainerImageEscaped(dockerRegistry, dockerRepository, projectName, version) {
    def containerImage = formatContainerImage(dockerRegistry, dockerRepository, projectName, version)
    return escape(containerImage)
}

static String escape(String str) {
    return str.replaceAll("/", "\\\\/")
                .replaceAll("\\.", "\\\\.")
}

def createDirectory(path) {
    try {
        sh("mkdir -p $path")
    } catch (e) {
        error("Falha ao criar diretorio $path. Erro: ${e.message}")
    }
}

def removeDirectory(path) {
    def relativePath = "./$path"
    try {
        sh("rm -rf $relativePath")
    } catch (e) {
        error("Falha ao remover diretório $relativePath. Erro: ${e.message}")
    }
}

def copyFile(sourceFile, targetPath) {
    try {
        sh("cp $sourceFile $targetPath")
    } catch (e) {
        error("Falha ao copiar arquivo de $sourceFile para $targetPath. Erro: ${e.message}")
    }
}

def formatReleaseCandidateTag() {
    def version = getVersion()
    return "v${version}-rc"
}

def formatPromotedReleaseTag() {
    def version = getVersion()
    return "v${version}"
}

def formatDevelopmentTag() {
    def version = getVersion()
    return "${version}-BUILD-${currentBuild.number}"
}

static def isDevelopmentTag(version) {
    return version.contains("BUILD")
}

def isNotAlreadyPromoted() {
    def tag = formatPromotedReleaseTag()
    try {
        def status = sh(script: """git ls-remote --tags origin | grep -v "\\-rc" | grep "${tag}" && exit 1 || exit 0""", returnStatus: true)
        return status == 0
    } catch (e) {
        error("Falha ao verificar se tag $tag está promovida. Erro: ${e.message}")
    }
}

def createGitTag(tag) {
    try {
        sh "git tag ${tag}"
        sh "git push --tags"
    } catch (e) {
        error("Falha ao criar tag $tag no git! Erro: ${e.message}")
    }
}

def findLatestVersionFromCurrentSeries() {
    def version = getVersion()
    def series = getSeriesFromVersion(version)
    try {
        sh "git fetch --tags --all"
        def lastTagFromSeries = sh(script: """git tag -l --sort=version:refname 'v${series}*' | tail --lines=1 | tr -d '\\n'""", returnStdout: true)
        // ex. de retorno: "1.2.3"
        return lastTagFromSeries.minus("v").minus("-rc")
    } catch (e) {
        error("Falha ao identificar tag mais recente da serie $series. Erro: ${e.message}")
    }
}

@NonCPS
def getSeriesFromVersion(version) {
    def pattern = Pattern.compile("^(?<version>\\d+\\.\\d+\\.)\\d+")
    def matcher = pattern.matcher(version)

    if (!matcher.find()) {
        error("Falha ao aplicar Matcher para detectar a serie da git tag")
    }
    // ex. de retorno: "1.2." (sem a ultima casa)
    return matcher.group(1)
}

def discardChangesFromFile(file) {
    try {
        sh "git checkout HEAD $file"
    } catch (e) {
        error("Falha ao descartar alterações no arquivo $file. Erro: ${e.message}")
    }
}

def getVersion() {
    return getProjectProperty("version")
}

def getProjectName() {
    return getProjectProperty("projectName")
}

def getDockerRegistry() {
     return getProjectProperty("dockerRegistry")
}

def getDockerImageRepository() {
     return getProjectProperty("dockerImageRepository")
}

def getDockerImageName() {
    def props = getProjectProperties()
    def property = props["dockerImageName"]
    if (!property) {
        return getProjectName()
    }
    return property
}

def getProjectProperty(propertyName) {
    def props = getProjectProperties()
    def property = props[propertyName]
    if (!property) {
        error("A propriedade \"$propertyName\" não foi encontrada no \"$propertiesFileName\"!")
    }
    return property
}

def getProjectProperties() {
    def filePath = "${pwd()}/$propertiesFileName"
    if (!fileExists(filePath)) {
        error("O arquivo de propriedades \"$propertiesFileName\" não foi encontrado na raiz do projeto!")
    }
    readProperties file: filePath
}

def setVersion(newVersion) {
    def currVersion = getVersion()
    replaceProjectPropertyValue(currVersion, newVersion)
}

def replaceProjectPropertyValue(oldValue, newValue) {
    try {
        sh "sed -i 's/$oldValue/$newValue/g' $propertiesFileName"
    } catch (e) {
        error("Falha ao substituir propriedade de $oldValue para $newValue no $propertiesFileName. Erro: ${e.message}")
    }
}

def merge(sourceBranch, targetBranch) {
    try {
        sh "git fetch origin $targetBranch"

        sh "git fetch origin $sourceBranch"

        sh "git checkout $targetBranch"

        sh "git merge origin/$sourceBranch --no-ff"

        sh "git push origin $targetBranch"

        sh "git checkout $sourceBranch"
    } catch (e) {
        error("Falha ao mergear o branch $sourceBranch no branch $targetBranch. Erro: ${e.message}")
    }
}

def commit(branch, message) {
    try {
        sh "git commit --all --message \"$message\" --allow-empty"
        sh "git push origin HEAD:$branch"
    } catch (e) {
        error("Falha ao realizar commit do branch $branch com a mensagem $message. Erro: ${e.message}")
    }
}

def checkout(branch) {
    try {
        sh "git fetch"
        sh "git checkout $branch"
    } catch (e) {
        error("Falha ao realizar checkout do branch $branch. Erro: ${e.message}")
    }
}

def deleteBranch(branch) {
    try {
        sh "git push -u origin -d $branch"
    } catch (e) {
        error("Falha ao deletar o branch remoto $branch. Erro: ${e.message}")
    }
}

boolean isAwsDeploy() {
    return fileExists("ecs")
}

boolean isPagCloudDeploy() {
    return fileExists("k8s")
}

boolean isFortifyEnabled() {
    if (!env.FORTIFY_ENABLED) {
        return true
    }
    return Boolean.parseBoolean(env.FORTIFY_ENABLED)
}

boolean isSonarEnabled() {
    if (!env.SONAR_ENABLED) {
        return true
    }
    return Boolean.parseBoolean(env.SONAR_ENABLED)
}

def sendNewRelicDeployNotification(newRelicAccount, revision) {
    def projectName = getProjectName()
    result = build(job: "COMMONS/newrelic-deploy-notification",
        parameters: [
            [
                $class: "StringParameterValue",
                name  : "NEWRELIC_ACCOUNT",
                value : newRelicAccount
            ],
            [
                $class: "StringParameterValue",
                name  : "APP_NAME",
                value : projectName
            ],
            [
                $class: "StringParameterValue",
                name  : "REVISION",
                value : revision
            ]
        ],
        propagate: false
    ).result
}

def notifyDeployStarted() {
    notificationUtils.sendMsg("Iniciando a Entrega.")
}

return this

