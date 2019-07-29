pipeline {
  agent any
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
