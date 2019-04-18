# Pipelines and Jenkins on OpenShift

OpenShift comes with a Jenkins image that is opiniated and preconfigured to allows jobs to be scheduled on the Kubernetes cluster. Moreover, the following features all together create a very good user experience when using Jenkins together with OpenShift:

- Jenkins is configured for OAuth2 based Authentication and Authorization towards Openshift
- Jenkins can schedule slave pods with different build tools on demand
- Builds with *Pipeline* strategy are executed by Jenkins without any interaction on Jenkins, one console to rule them all
- Objects such as ConfigMaps and Secrets from OpenShift are sycned/copied into Jenkins

## CI/CD Principles in a nutshell
When dealing with containers in a CI/CD settings the following principles are very important:

- Container images should be built once and only once. If you have to build different images per stage, something went wrong :(
- For configuring stage dependant things such as database credentials & coordinates, MQ related settings and general application configuration, use environment values, configuration files that are populated via ConfigMaps and/or Secrets.
- Container images should be tagged so that matching software/code is visible
- Changes from one stage to the next, should be applied via automation and without any manual intervention.

## Pipelines

One of the primary build strategies that come with the OpenShift Container Platform is called the **Pipeline**.

The Pipeline build strategy can be used to implement sophisticated workflows:
- continuous integration
- continuous deployment

The Pipeline build strategy allows developers to define a [Jenkins pipeline](https://jenkins.io/doc/pipeline/) for execution by the Jenkins pipeline plugin. The build can be started, monitored, and managed by OpenShift Container Platform in the same way as any other build type.

Pipeline workflows are defined in a Jenkinsfile, either embedded directly in the build configuration, or supplied in a Git repository and referenced by the build configuration.

## First pipeline

You should have access to a project called **cicd-userXY**  , switch to thi project via:

```bash
oc project cicd-userXY
```

**UserXY** will have admin rights on project **cicd-userXY**

In this project, there should be a route with name jenkins. Let's see what routes exist in the project:

```bash
[root@bastion ~]# oc get routes
NAME      HOST/PORT                                                   PATH      SERVICES   PORT      TERMINATION     WILDCARD
jenkins   jenkins-cicd-userXY.apps.zurich-fdbf.openshiftworkshop.com             jenkins    <all>     edge/Redirect   None
```

Now copy the url from this route, in this case *jenkins-cicd-userXY.apps.zurich-fdbf.openshiftworkshop.com* from the route definition and open this url in a browser. **!Replace the route url with what you get from your own execution**

The first time you hit a OpenShift Jenkins with a specific user, you might get a Jenkins screen asking for authorization permissions.
See the image below.
![Jenkins OAuth2 permissions](data/images/jenkins_oauth2_permissions.png "Jenkins OAuth2 permissions")




Accept these and go to the next screen, by clicking on 'Alow selected permissions'.

Next screen, should be the famous/classical (or infamous) Jenkins welcome screen.


![Jenkins welcome](data/images/jenkins_welcome_screen.png "Jenkins welcome")

### Step 1
Now that we have confirmed Jenkins master is up and running, we can start creating a first pipeline. Openshift pipelines are just BuildConfig objects with a special stragety.

Copy the content below and write it into a file called **bc_first-pipeline.yaml**

```
apiVersion: v1
kind: BuildConfig
metadata:
  name: first-pipeline
spec:
  strategy:
    jenkinsPipelineStrategy:
      jenkinsfile: |-
        pipeline {
          stages {
            stage("Hello") {
              steps {
               sh 'echo "Hello World!"'
              }
            }
          }
        }
    type: JenkinsPipeline
  triggers: []
```

**Note**: The pipeline above is a declarative Jenkins pipeline. For seeing the differences between declarative vs scripting see [here](https://jenkins.io/doc/book/pipeline/syntax/#compare)

and let's create a resource based on this file on the cicd project.
```
oc create -f bc_first-pipeline.yaml 
```

As soon as the pipeline is created, we can start it with the following command:
```
oc start-build first-pipeline
```

Now go to OpenShift web console and go to pipelines view by clicking on Builds>Pipelines from the menu shown on left, see the image below:
![Pipeline link](data/images/console_pipeline_link.png "Pipeline link")

You should see that first run/build of the pipeline has failed:
![Pipeline failed](data/images/failed_pipeline_agent.png "")

Now click on the 'View Log' link, to jump to Jenkins build logs. See the image below which highlights the 'View Log' link.
![Pipeline log link](data/images/view_pipeline_log.png "") 

In this screen we have an error message which says 'agent section' is missing.

![jenkins_missing_agent_error.png](data/images/jenkins_missing_agent_error.png "") 

Agent is a reuired field for a declarative pipeline, you can read [here](https://jenkins.io/doc/book/pipeline/syntax/#agent) for details.

Let's add the following section to our pipeline to set an agent.

```
  agent {
    label 'master'
  }
```

whole pipeline would look like this:

```
pipeline {
  agent {
    label 'master'
  }
  stages {
    stage("Clone Source") {
      steps {
       sh 'echo "Hello World!"'
      }
    }
  }
}
```

This means, we would like pipeline steps to be executed in the master. After updating the pipeline, run the pipeline again. This time it should finish successfully. See the image below for example output from a successful run:


![agent_successful_run_log.png](data/images/agent_successful_run_log.png "") 

It's up to the Jenkins author to decide where each step of a Pipeline should be executed. Depending on what tools are used, you should select the right agent. For example, if you want to run a maven task, then maven should be available on the target agent where the maven step is executed.

### Step 2
Let's continue to extend our pipeline.

This time, we would like to checkout some source code and create a deployable artifact.
We will update the existing pipeline ( **first-pipeline** ) with the following pipeline definition.

```
pipeline {
  agent {
    label 'maven'
  }
  stages {
    stage("Clone Source") {
      steps {
       git 'https://github.com/mcelep/example-spring-boot-helloworld'
      }
    }
    
    stage("Mvn build"){
        steps{
            sh "mvn clean package -DskipTests"
        }
    }
  }
}
```

There are couple of things to notice here:
- As target agent instead of **master** , **maven** is used. This will cause a new pod to be scheduled, if there is no existing one.
- All the steps are executed on the agent that is specified at the pipeline level.
- There are two steps now. The first one is for checking out the source code. The second step, spawns a new shell to trigger maven goals.
  
Run the pipeline again. Either via oc cli ( oc start-build) or OpenShift Web Console or alternatively directly via the Jenkins UI. You will notice that Jenkins and Openshift will be consistent regarding Job definitions and status i.e. you can also start a Jenkins Job via the 'Build Now' button on Jenkins and the job run should syncrhonized a build back to OpenShift.

The cli command for starting a new build:
```
oc start-build first-pipeline
```

In order to see the creation of the new maven pod/slave/agent, the following command can be used:
```
oc get pods -w
```

example output:
```
NAME              READY     STATUS    RESTARTS   AGE
jenkins-1-dqzmg   1/1       Running   0          4h
maven-v8j2m   0/1       Pending   0         0s
maven-v8j2m   0/1       Pending   0         0s
maven-v8j2m   0/1       ContainerCreating   0         0s
maven-v8j2m   1/1       Running   0         5s
maven-v8j2m   0/1       Terminating   0         1m
maven-v8j2m   0/1       Terminating   0         1m
maven-v8j2m   0/1       Terminating   0         1m
```

**Note:** After build was complete the newly created maven pod (*maven-v8j2m* in the example above) got terminated.
The behavior of how long a slave/agent pod stays around is controlled via Jenkins. Click on 'Manage Jenkins' on main Jenkins Screen, go to 'Configure System' search for 'Time in minutes to retain slave when idle', enter a value for this field if you want to keep slaves around even when they are not in use.


Check the build job logs again. This time you should see from the logs that maven ran and it downloaded pretty much the whole internet :) 
Example logs tail:
```
[INFO] Installing /tmp/workspace/cicd-user2/cicd-user2-first-pipeline/target/spring-boot-hello-world-0.1.0.jar to /home/jenkins/.m2/repository/com/appuio/techlab/spring-boot-hello-world/0.1.0/spring-boot-hello-world-0.1.0.jar
[INFO] Installing /tmp/workspace/cicd-user2/cicd-user2-first-pipeline/pom.xml to /home/jenkins/.m2/repository/com/appuio/techlab/spring-boot-hello-world/0.1.0/spring-boot-hello-world-0.1.0.pom
[INFO] [1m------------------------------------------------------------------------[m
[INFO] [1;32mBUILD SUCCESS[m
[INFO] [1m------------------------------------------------------------------------[m
[INFO] Total time: 57.224 s
[INFO] Finished at: 2019-04-18T11:44:16Z
[INFO] Final Memory: 34M/146M
[INFO] [1m------------------------------------------------------------------------[m
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // node
[Pipeline] End of Pipeline
Finished: SUCCESS
```

A jar artifact is ready to be used within a container.


**Note** For increasing build speed, more memory/cpu can be provided to slave pod.

**Note** To persist (and thus to avoid download) dependencies through different job runs, see [Slaves with PVs](#Slaves-with-PVs)

### Step 3

We have 4 OpenShift projects for each user:
- cicd-userXY : Jenkins and slaves
- app-dev-userXY: Stage development
- app-int-userXY: Stage integration
- app-prod-userXY: Stage production


Now that there is an artifact, namely a jar file that we would like to run, we need to create a container image from this jar file. A docker build with binary input seems appropriate for the job. We should build a container image in the **DEV** stage. So Use the following commands to create a bc, dc, service and route:

```
oc project app-dev-userXY
oc new-build --name=spring-app --strategy=docker --dockerfile="FROM java:8\n COPY . /deployments"
oc new-app spring-app
oc expose svc/spring-app
```

After having prepared the application on Dev stage, pipeline can be updated with the following content:
```
pipeline {
  agent {
    label 'maven'
  }
  
  environment {
    app = "spring-app"
    devProject = 'app-dev-user2'
  }
  
  stages {
    stage("Clone Source") {
      steps {
       git 'https://github.com/mcelep/example-spring-boot-helloworld'
      }
    }
    
    stage("Mvn build"){
        steps{
            sh "mvn clean install -DskipTests"
            sh "mv target/*.jar target/artifact.jar"
        }
    }
    
    stage('Build image on Openshift') {
      steps {
        script {
          openshift.withProject("${devProject}") {
            println "Building latest on dc:${app} on project:${devProject}"
            openshift.selector("bc", app).startBuild("--from-file=target/artifact.jar", "--wait")
          }
        }
      }
    }
  }
}
```

Start the pipeline again and see that it fails with a message like:
```
ERROR: Error running start-build on at least one item: [buildconfig/spring-app];
{reference={}, err=Uploading file "target/artifact.jar" as binary input for the build ...

Uploading finished
Error from server (Forbidden): buildconfigs.build.openshift.io "spring-app" is forbidden: User "system:serviceaccount:cicd-userXY:jenkins" cannot create buildconfigs.build.openshift.io/instantiatebinary in the namespace "app-dev-userXY": no RBAC policy matched, verb=start-build, cmd=oc --server=https://172.30.0.1:443 --certificate-authority=/var/run/secrets/kubernetes.io/serviceaccount/ca.crt --namespace=app-dev-userXY --token=XXXXX start-build buildconfig/spring-app --from-file=target/artifact.jar --wait -o=name , out=, status=1}

Finished: FAILURE
```

The reason for this failure is that, CICD project service account which jenkins slave runs with does not have the right permissions on the target project i.e. DEV in this case. Assign the correct rights by using `oc policy add-role-to-user` command. See [Openshift rights](Openshift-rights) section for details. *Edit* role should suffice for enabling Jenkins in this particular case.
Example command:
```
oc policy add-role-to-user edit system:serviceaccount:cicd-userXY:jenkins -n app-dev-userXY
```

Run the build again and this time build should succeed.













## General tips and tricks

###Slaves with PVs
Jenkins slaves which run as Pods are by default stateless i.e. if there are artifacts or other binaries that you would like to keep even when a Pod gets restarted, Persistent Volumes should be used. Make sure that artifact folder used by maven is on the persistent volume. To force maven to use a specific folder,  you can configure mvn on the fly via: ``` mvn -Dmaven.repo.local=$HOME/.my/other/repository clean install ``` or via  the *setting.xml* file.

###Slave retention/idle time
 Jenkins slave that are created on demand will be terminated after they are done building jobs and sometimes you want to keep slaves around even when they are not doing any work so that you don't need to wait until a slave is created and registered on Jenkins master. 
 The retention policy and  setting ***Time in minutes to retain agent when idle*** which is listed under 'Kubernetes Pod Template', can be used to control how long you keep unused/idle slaves around.
![Slave Pod retention](data/images/pod_retention.png "Slave Pod retention")
 
### OpenShift Jenkins Sync
[Openshift-jenkins-sync-plugin](https://github.com/openshift/jenkins-sync-plugin/blob/master/README.md) can sync objects such as Secrets,ConfigMaps from OpenShift projects onto Jenkins. This is a very powerful feature and it's also the main enabler of pipeline strategy builds. One typical use case is to keep credentials such git ssh keys as secrets and have Jenkins sync them so that these credentials can be used in build jobs.

For syncing secrets please make sure that secrets are labeled accordingly.
 
```  oc label secret jboss-eap-quickstarts-github-key credential.sync.jenkins.openshift.io=true```



### OpenShift Client Plugins
Jenkins slaves/masters can run on one cluster and they can interact with other clusters. See [jenkins-client-plugin](https://github.com/openshift/jenkins-client-plugin) for details.
In order to address multiple clusters, first configure them on Jenkins via *Manage Jenkins-> Configure Systems->OpenShift Jenkins Sync* as shown in image below.
![Jenkins sync plugin](data/images/jenkins_sync.png "Jenkins sync plugin")

### Openshift rights
All pods on OpenShift run with a ServiceAccount and the service account that 'runs' a job should have the rights set up according to what actions it aims to execute on the target namespace/project.

```
oc policy add-role-to-user edit system:serviceaccount:$CICD_PROJECT:$SA -n $TARGET_PROJECT


$CICD_PROJECT is the OpenShift project on which Jenkins job runs.
$SA sets the service account which runs the Jenkins job.
$TARGET_PROJECT is where OpenShift object you interact with resides on.
```
