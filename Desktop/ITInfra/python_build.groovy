def call () {
    pipeline {
	agent { label 'python_agent' }
	stages {
		stage('Setup'){
			steps{
				echo "Running ${env.BUILD_ID} with workspace ${env.WORKSPACE}"

				script {
					def foldersToDelete = ['test-reports','api-test-reports','venv']
						for (folder in foldersToDelete) {
							if (fileExists(folder)) {
								dir(folder) {
									deleteDir()
									}
								echo "Deleted directory: ${folder}"
							}
							else {
								echo "Directory not found, skipping: ${folder}"
							}
						}
					}

					sh 'ls -la' 

					sh 'python3 -m venv venv'
					sh 'venv/bin/pip install coverage'
			}
		}
		stage('Build') {
			steps {
				sh 'venv/bin/pip install -r requirements.txt'
			}
		}
		stage('Python Lint') {
			steps {
				sh 'pylint --fail-under 5 *.py'
			}
		}	
		stage('Test and Coverage') {
			steps {
				script {
					def tfs = findFiles(glob: 'test*.py')

					for (tf in tfs) {						
						sh "./venv/bin/coverage run --omit '*/site-packages/*,*/dist-packages/*' ${tf.path}"
						
					}
					sh './venv/bin/coverage report'
				}
			}
			post {
				always{
					script {
					def test_reports_exist = fileExists 'test-reports'
						if (test_reports_exist) {
							junit 'test-reports/*.xml'
							}

					def api_test_reports_exist = fileExists 'api-test-reports'
						if (api_test_reports_exist) {
							junit 'api-test-reports/*.xml'
						}
					}

				}
				
		}
	}
			stage('Zip Artifacts') {
			steps {
				sh 'zip app.zip *.py'
			}
			post {
				always {
				archiveArtifacts artifacts: 'app.zip'
				}
			}
		}
}
}
}