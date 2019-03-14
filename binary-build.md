# Binary Build

This example describes how to deploy a web archive (war) in Wildfly using the OpenShift client (oc) in binary mode.
The example is inspired by APPUiO blog: <http://docs.appuio.ch/en/latest/app/wildflybinarydeployment.html>

## Create a new project

```bash
oc new-project binary-build-userXY
```

## Create the deployment folder structure

Prepare a temporary folder and create the deployment folder structure inside.

One or more war can be placed in the deployments folder. In this example an existing war file is downloaded from a Git repository:

```bash
mkdir tmp-bin
cd tmp-bin
mkdir deployments
wget -O deployments/ROOT.war 'https://gogs.apps.0x2shift.dev/openshift/techlab/blob/master/data/hello-world-war-1.0.0.war?raw=true'
```

## Create a new build using the Wildfly image

The flag *binary=true* indicates that this build will use the binary content instead of the url to the source code.

Command:

```bash
oc new-build --docker-image=openshift/wildfly-160-centos7 --binary=true --name=hello-world -l app=hello-world
```

Command with output:

```bash
$ oc new-build --docker-image=openshift/wildfly-160-centos7 --binary=true --name=hello-world -l app=hello-world
--> Found Docker image 5c7681e (19 hours old) from Docker Hub for "openshift/wildfly-160-centos7"

    WildFly 16.0.0.Final 
    -------------------- 
    Platform for building and running JEE applications on WildFly 16.0.0.Final

    Tags: builder, wildfly, wildfly16

    * An image stream will be created as "wildfly-160-centos7:latest" that will track the source image
    * A source build using binary input will be created
      * The resulting image will be pushed to image stream "hello-world:latest"
      * A binary build was created, use 'start-build --from-dir' to trigger a new build

--> Creating resources with label app=hello-world ...
    imagestream "wildfly-160-centos7" created
    imagestream "hello-world" created
    buildconfig "hello-world" created
--> Success
```

See the command output for the created resources.

Check the created resources with the oc tool and inside the web console.

## Start the build

To trigger a build issue the command below. In a continuous deployment process this command can be repeated
whenever there is a new binary or a new configuration available.

The core feature of the binary build is to provide the files for the build from the local directory.
Those files will be loaded into the build container that runs inside OpenShift.

```bash
oc start-build hello-world --from-dir=. --follow
```

The parameter _--from-dir=._ tells the oc tool which directory to upload.

The _--follow_ flag will show the build log on the console and wait until the build is finished.

## Create a new app

```bash
oc new-app hello-world -l app=hello-world
```

See the command output for the created resources.

Check the created resources with the oc tool and inside the web console.
Try to find out, if the wildfly has started.

## Expose the service as route

```bash
oc expose svc hello-world
```

Inside the web console click onto the route to see the output of the hello-world application.
