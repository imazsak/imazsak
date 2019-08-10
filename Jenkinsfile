pipeline {
  agent {
    label 'docker-compose'
  }
  environment {
    COMPOSE_PROJECT_NAME = "${env.BUILD_TAG}"
  }
  stages {
    stage('Compile') {
      steps {
        sh "docker-compose build"
      }
    }
    stage('Compose up') {
      steps {
        sh "docker-compose up -d"
      }
    }
    stage('Test') {
      steps {
        sh "docker-compose exec -T imazsak sbt test"
      }
    }
    stage('IT Test') {
      steps {
        sh "docker-compose exec -T imazsak sbt it:test"
      }
    }
    stage('Build image') {
      steps {
        sh "docker build -t imazsak-core:${env.BUILD_ID} --build-arg COMPOSE_PROJECT_NAME=${COMPOSE_PROJECT_NAME} -f Dockerfile-build ."
      }
    }
    stage('Tag & Push') {
      steps {
        sh "docker tag imazsak-core:${env.BUILD_ID} rg.fr-par.scw.cloud/imazsak/imazsak-core:${env.GIT_COMMIT}"
        script {
          withDockerRegistry(credentialsId: 'scaleway-imazsak-registry', url: 'https://rg.fr-par.scw.cloud/imazsak') {
            docker.image("rg.fr-par.scw.cloud/imazsak/imazsak-core:${env.GIT_COMMIT}").push()
          }
        }
      }
    }
    stage('Stage Deploy') {
      steps {
        script {
          sshagent (credentials: ['github-jenkins-imazsak']) {
            sh """
              rm -R infra || true
              GIT_SSH_COMMAND="ssh -o StrictHostKeyChecking=no" git clone git@github.com:Ksisu/imazsak-stage-infra.git infra && cd infra
              sed -i "s|\\(image: rg.fr-par.scw.cloud/imazsak/imazsak-core\\).*|\\1:${env.GIT_COMMIT}|" ./core/core.yml
              git add ./core/core.yml
              git config user.email "ci@imazsak.hu"
              git config user.name "Jenkins"
              git commit -m "Upgrade core ${env.GIT_COMMIT}" || true
              GIT_SSH_COMMAND="ssh -o StrictHostKeyChecking=no" git push git@github.com:Ksisu/imazsak-stage-infra.git master
            """
          }
          sshagent (credentials: ['imazsak-stage-vm']) {
            sh """
              ssh -o StrictHostKeyChecking=no root@stage.imazsak.hu "cd /opt/imazsak-stage && git pull && docker stack deploy --compose-file ./core/core.yml --with-registry-auth --prune core"
            """
          }
        }
      }
    }
  }
  post {
    always {
      sh """
        docker-compose exec -T imazsak cp -R ./target/test-reports ${WORKSPACE}/test-reports || true
        docker-compose exec -T imazsak chmod 777 -R ${WORKSPACE}/test-reports || true
      """
      sh "docker-compose down -v || true"
      junit 'test-reports/**/*.xml'
    }
  }
}
