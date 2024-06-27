#!/usr/bin/env groovy

def getFile(organization, repositoryName, branch, path) {
    withCredentials([
        usernamePassword(
                credentialsId: 'svcaccgithub',
                passwordVariable: 'GITHUB_TOKEN',
                usernameVariable: 'GITHUB_USER'
        )
    ]) {
        // https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28#get-repository-content
        def response = sh(script:"""
            curl \
                -H "X-GitHub-Api-Version: 2022-11-28" \
                -H "authorization: Bearer ${GITHUB_TOKEN}" \
                -H "Content-Type: application/vnd.github.raw" \
                https://api.github.com/repos/${organization}/${repositoryName}/contents/${path}?ref=${branch}
        """, returnStdout: true).trim()

        echo "${response}"

        def res = readJSON text: response

        if (!res.download_url) {            
            return null
        }

        return sh(script:"""
            curl \
                -H "X-GitHub-Api-Version: 2022-11-28" \
                -H "authorization: Bearer ${GITHUB_TOKEN}" \
                -H "Content-Type: text/plain" \
                ${res.download_url}
            """, returnStdout: true).trim()
    }
}

return this