pipeline {
    agent any

    tools {
        nodejs 'node20'
    }

    environment {
        IMAGE_NAME = 'ghandgevikas/my-app'
        TAG = 'v1'
        REGION = 'ap-south-1'
        SCANNER_HOME = tool 'sonar-scanner'
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout Code') {
            steps {
                git branch: 'creds-troubleshooting', url: 'https://github.com/Vikasghandge/Job-Portal.git'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar-server') {
                    sh '''${SCANNER_HOME}/bin/sonar-scanner \
                      -Dsonar.projectName=Job-portal \
                      -Dsonar.projectKey=Job-portal'''
                }
            }
        }

        stage('Install Dependencies') {
            steps {
                dir('job-portal-main') {
                    sh 'npm install'
                }
            }
        }

        stage('Docker Scout FS') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker', toolName: 'docker') {
                        sh 'docker-scout quickview fs://.'
                        sh 'docker-scout cves fs://.'
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                dir('job-portal-main') {
                    script {
                        withCredentials([string(credentialsId: 'docker-hub-token', variable: 'DOCKER_TOKEN')]) {
                            sh '''
                                echo "$DOCKER_TOKEN" | docker login -u "ghandgevikas" --password-stdin
                                docker build -t $IMAGE_NAME:$TAG .
                            '''
                        }
                    }
                }
            }
        }

        stage('Docker Push') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'docker-hub-token', variable: 'DOCKER_TOKEN')]) {
                        sh '''
                            echo "$DOCKER_TOKEN" | docker login -u "ghandgevikas" --password-stdin
                            docker push $IMAGE_NAME:$TAG
                        '''
                    }
                }
            }
        }

        stage('Run Container') {
            steps {
                sh 'docker run -d --name my-con1 -p 3000:3000 $IMAGE_NAME:$TAG'
            }
        }
    }
}
