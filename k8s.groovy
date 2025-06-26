pipeline {
    agent any 

    tools {
        nodejs 'node18'
    }

    environment {  //
        IMAGE_NAME = 'ghandgevikas/my-app'
        TAG = 'v1'
        REGION = 'ap-south-1'
        SCANNER_HOME = tool 'sonar-scanner'
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()  // 
            }
        }

        stage('Checkout Code') {
            steps {
                git branch: 'test', url: 'https://github.com/Vikasghandge/Job-Portal.git'
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

        stage('Quality Gate') {
            steps {
                script {
                    waitForQualityGate abortPipeline: true, credentialsId: 'Sonar-token'
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
                    sh 'docker build -t $IMAGE_NAME:$TAG .'
                }
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh 'echo $DOCKER_PASSWORD | docker login --username $DOCKER_USERNAME --password-stdin'
                }
            }
        }

        stage('Docker Push') {
            steps {
                sh 'docker push $IMAGE_NAME:$TAG'
            }
        }

        // stage('Run Container') {
        //     steps {
        //         sh 'docker run -d --name my-con1 -p 3000:3000 $IMAGE_NAME:$TAG'  // ✅ Fix: use the tag
        //     }
        // }
        stage('Deploy on k8s') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'k8s']]) {
                    dir('FullStack-Blogging-App-main/K8S') {
                        sh '''
                            aws eks --region ap-south-1 update-kubeconfig --name EKS_CLOUD
                            kubectl apply -f deployment.yml
                            kubectl apply -f service.yml
                        '''
                    }
                }
            }
        }
    }
}
