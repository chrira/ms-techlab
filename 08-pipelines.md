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


Let's continue to extend our pipeline.

This time, we would like to checkout some source code and create a deployable artifact.

```

```






## Some tips and tricks

###Slaves with PVs
Jenkins slaves which run as Pods are by default stateless i.e. if there are artifacts or other binaries that you would like to keep even when a Pod gets restarted, Persistent Volumes should be used. Make sure that artifact folder used by maven is on the persistent volume. To force maven to use a specific folder,  you can configure mvn on the fly via: ``` mvn -Dmaven.repo.local=$HOME/.my/other/repository clean install ```

###Pipeline Strategy
One of the build strategy options that comes with OpenShift is called [Pipeline Strategy](https://docs.openshift.com/container-platform/3.11/dev_guide/builds/build_strategies.html#pipeline-strategy-options).

Creating a *BuildConfig* object with Pipelinestrategy is not obligatory to use Jenkins pipelines, however when such a  *BuildConfig* is created and Jenkins is configured correctly, Jenkins jobs can be controlled via OpenShift. Instead of triggering a new build by using the Jenkins interface, OpenShift can trigger a new build just as it can be done for other types *BuildConfigs* such as [S2I](https://docs.openshift.com/container-platform/3.11/architecture/core_concepts/builds_and_image_streams.html#source-build). 
Moreover, Jenkins job statuses and progress will also be visible on the OpenShift console. See the image below. Each stage of a pipeline is displayed with the corresponding name from Jenkins pipeline together with the duration.

![Pipelines on OpenShift console](pipelines.png "Pipelines on OpenShift console")

###Slave retention/idle time
 Jenkins slave that are created on demand will be terminated after they are done building jobs and sometimes you want to keep slaves around even when they are not doing any work so that you don't need to wait until a slave is created and registered on Jenkins master. 
 The retention policy and  setting ***Time in minutes to retain agent when idle*** which is listed under 'Kubernetes Pod Template', can be used to control how long you keep unused/idle slaves around.
![Slave Pod retention](pod_retention.png "Slave Pod retention")
 

### OpenShift Jenkins Sync
[Openshift-jenkins-sync-plugin](https://github.com/openshift/jenkins-sync-plugin/blob/master/README.md) can sync objects such as Secrets,ConfigMaps from OpenShift projects onto Jenkins. This is a very powerful feature and it's also the main enabler of pipeline strategy builds. One typical use case is to keep credentials such git ssh keys as secrets and have Jenkins sync them so that these credentials can be used in build jobs.

For syncing secrets please make sure that secrets are labeled accordingly.
 
```  oc label secret jboss-eap-quickstarts-github-key credential.sync.jenkins.openshift.io=true```



### OpenShift Client Plugins
Jenkins slaves/masters can run on one cluster and they can interact with other clusters. See [jenkins-client-plugin](https://github.com/openshift/jenkins-client-plugin) for details.
In order to address multiple clusters, first configure them on Jenkins via *Manage Jenkins-> Configure Systems->OpenShift Jenkins Sync* as shown in image below.
![Jenkins sync plugin](jenkins_sync.png "Jenkins sync plugin")

### Openshift rights
All pods on OpenShift run with a ServiceAccount and the service account that 'runs' a job should have the rights set up according to what actions it aims to execute on the target namespace/project.

```
oc policy add-role-to-user edit system:serviceaccount:$CICD_PROJECT:$SA -n $TARGET_PROJECT
```


\$CICD_PROJECT is the OpenShift project on which Jenkins job runs.
\$SA sets the service account which runs the Jenkins job.
\$TARGET_PROJECT is where OpenShift object you interact with resides on.







