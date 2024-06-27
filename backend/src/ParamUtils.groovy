#!/usr/bin/env groovy

static def tagSelectionParameter(params) {
    return singleSelectChoiceParameter(params, '''
def repositoryName = "${REPOSITORY_NAME}".trim()
def tagPattern = /^v(\\d+)\\.(\\d+)\\.(\\d+)$/
return ("git ls-remote --tags git@github.com:${ORGANIZATION}/${repositoryName}.git")
      .execute()
      .text
      .replaceAll('refs/tags/', '')
      .readLines()
      .collect { it.split()[1] }
      .findAll { it.matches(tagPattern) }
      .reverse()
            ''')
}

static def branchSelectionParameter(params) {
    return singleSelectChoiceParameter(params, '''
def repositoryName = "${REPOSITORY_NAME}".trim()
return ("git ls-remote --heads git@github.com:${ORGANIZATION}/${repositoryName}.git")
      .execute()
      .text
      .replaceAll('refs/heads/', '')
      .readLines()
      .collect { it.split()[1] }
            ''')
}

static def hotfixSelectionParameter(params) {
    return singleSelectChoiceParameter(params, '''
def repositoryName = "${REPOSITORY_NAME}".trim()
def hotfixPattern = /^(hotfix|HOTFIX)\\/.*$/
return ("git ls-remote --heads git@github.com:${ORGANIZATION}/${repositoryName}.git")
      .execute()
      .text
      .replaceAll('refs/heads/', '')
      .readLines()
      .collect { it.split()[1] }
      .findAll { it.matches(hotfixPattern) }
            ''')
}

static def singleSelectChoiceParameter(params, script) {
    return [$class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: "${params['description']}",
            filterLength: 1,
            filterable: true,
            name: "${params['name']}",
            randomName: 'choice-parameter-112531553894737',
            referencedParameters: "${params['dependsOn']}",
            script: [
                $class: 'GroovyScript',
                fallbackScript: [ classpath: [], sandbox: true, script: '[]' ],
                script: [
                    classpath: [],
                    sandbox: true,
                    script: script
                ]
            ]
    ]
}
