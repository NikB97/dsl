def GIT_URL = "https://github.com/MNT-Lab/mntlab-dsl"
def GIT_REPO =  "MNT-Lab/mntlab-dsl"
def GITHUB_BRANCH = "nbuzin"
def STUDENT = "nbuzin"
def JOB_LIST = []


def GROOVYSCRIPT = """
def command = "git ls-remote -h ${GIT_URL}"
def proc = command.execute()
proc.waitFor()
def branches = proc.in.text.readLines().collect {
  it.replaceAll(/[a-z0-9]*\trefs\\/heads\\//, '')
}
return branches
"""

//CHILD JOBS

for (i in 1..4) {
    JOB_LIST << "MNTLAB-${STUDENT}-child${i}-build-job"
    job("${JOB_LIST.last()}"){
       label("EPBYMINW2629")
        wrappers {
            preBuildCleanup()
        }
        configure {
        project->
            project / 'properties' << 'hudson.model.ParametersDefinitionProperty' {
            parameterDefinitions {
              'com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition' {
                  name 'BRANCH_NAME'
                  quoteValue 'false'
                  saveJSONParameterToFile 'false'
                  visibleItemCount '15'
                  type 'PT_SINGLE_SELECT'
                  groovyScript GROOVYSCRIPT
                  defaultValue "nbuzin"
                  multiSelectDelimiter ','
                  }
              }
            }
        }
        scm{
            git {
                remote
                        {
                            github("MNT-Lab/mntlab-dsl", "https")
                        }
                branch("\$BRANCH_NAME") }
        }
      
        steps {
            shell("""bash ./script.sh > output.txt 
                     tar -cvzf \${BRANCH_NAME}_dsl_script_${i}-\${BUILD_NUMBER}.tar.gz output.txt  
                     cp \${BRANCH_NAME}_dsl_script_${i}-\${BUILD_NUMBER}.tar.gz ../MNTLAB-${STUDENT}-main-build-job""")
        }
        publishers {
            archiveArtifacts("*.tar.gz")
        }
    }
}

//MAIN JOB

job("MNTLAB-${STUDENT}-main-build-job") {
    label("EPBYMINW2629")
    
    configure {
    project->
        project / 'properties' << 'hudson.model.ParametersDefinitionProperty' {
        parameterDefinitions {
            'com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition' {
                name 'BRANCH'
                quoteValue 'false'
                saveJSONParameterToFile 'false'
                visibleItemCount '15'
                type 'PT_SINGLE_SELECT'
                groovyScript GROOVYSCRIPT
                defaultValue "'nbuzin'"
                multiSelectDelimiter ','
            }
            'com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition' {
                name 'BUILD_JOBS'
                quoteValue 'false'
                saveJSONParameterToFile 'false'
                visibleItemCount '15'
                type 'PT_CHECKBOX'
                groovyScript "${JOB_LIST.collect{"'$it'"}}"
                multiSelectDelimiter ','
            }
        }
    }
  }
      scm{
            git {
                remote
                        {
                            github("MNT-Lab/mntlab-dsl", "https")
                        }
                branch("\$BRANCH") }
        }
      steps {
          downstreamParameterized {
              trigger("\$BUILD_JOBS") {
                  block {
                      buildStepFailure('FAILURE')
                      failure('FAILURE')
                      unstable('UNSTABLE')
                  }
                  parameters {
                      predefinedProp('BRANCH_NAME', '\$BRANCH')
                  }
              }
    }
      publishers {
          archiveArtifacts("*.tar.gz")
        }
    }
      wrappers {
        preBuildCleanup()
    }
}

