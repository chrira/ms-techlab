# Builds

There are three different types of builds:

1. Source-To-Image (s2i)
2. Binary Builds
3. Container aka. Docker Builds

Let's have a look at the different kinds of builds

## Source-To-Image

Simplest way of getting started from a code base (e.g. Ruby, Python, PHP) to a running application bundled with all the dependencies.

It creates all the necessary build configs, deployment configs and even automatically exposes the application on a route.

First, create a project with the name `userXY-s2i`
<details><summary>create project command</summary>oc new-project userXY-s2i</details><br/>

Our example is based on a very simple PHP application hosted on APPUiO GitHub.
Create an app with the name s2i from this repository: <https://github.com/appuio/example-php-sti-helloworld.git>

Hint for the command help:

    oc new-app -h

<details><summary>create app command</summary>oc new-app https://github.com/appuio/example-php-sti-helloworld.git --name=s2i</details><br/>

Check the status of your project.
<details><summary>project status command</summary>oc status</details><br/>

Explore the different resources created by the new-app command.

To see something in the browser, create a route to access the application:

    oc create route edge --service=s2i

## Binary build

This example describes how to deploy a web archive (war) in Wildfly using the OpenShift client (oc) in binary mode.
The example is inspired by APPUiO blog: <http://docs.appuio.ch/en/latest/app/wildflybinarydeployment.html>

### Create a new project

Create a project with the name `userXY-binary-build`
<details><summary>create project command</summary>oc new-project userXY-binary-build</details><br/>

### Create the deployment folder structure

Prepare a temporary folder and create the deployment folder structure inside.

One or more war can be placed in the deployments folder. In this example an existing war file is downloaded from a Git repository:

```bash
mkdir tmp-bin
cd tmp-bin
mkdir deployments
wget -O deployments/ROOT.war 'https://github.com/appuio/hello-world-war/blob/master/repo/ch/appuio/hello-world-war/1.0.0/hello-world-war-1.0.0.war?raw=true'
```

### Create a new build using the Wildfly image

Create a build configuration for a binary build with following attributes:

* base Docker image: `openshift/wildfly-160-centos7`
* name: `hello-world`
* label: `app=hello-world`.
* type: `binary`

The flag *binary=true* indicates that this build will use the binary content instead of the url to the source code.

Command:

```bash
oc new-build --docker-image=openshift/wildfly-160-centos7 --binary=true --name=hello-world -l app=hello-world
```

Command with output:

```bash
$ oc new-build --docker-image=openshift/wildfly-160-centos7 --binary=true --name=hello-world -l app=hello-world
--> Found Docker image 5b42148 (5 days old) from Docker Hub for "openshift/wildfly-160-centos7"

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

To trigger a build issue the command below. In a continuous deployment process this command can be repeated whenever there is a new binary or a new configuration available.

The core feature of the binary build is to provide the files for the build from the local directory.
Those files will be loaded into the build container that runs inside OpenShift.

```bash
oc start-build hello-world --from-dir=. --follow
```

The parameter _--from-dir=._ tells the oc tool which directory to upload.

The _--follow_ flag will show the build log on the console and wait until the build is finished.

### Create a new app

```bash
oc new-app hello-world -l app=hello-world
```

See the command output for the created resources.

Check the created resources with the oc tool and inside the web console.
Try to find out, if the wildfly has started.

### Expose the service as route

```bash
oc create route edge --service=hello-world
```

Inside the web console click onto the route to see the output of the hello-world application.

## Container Build

We can also create arbitrary containers based on Dockerfiles.

First, create a project with the name `userXY-docker-build`
<details><summary>create project command</summary>oc new-project userXY-docker-build</details><br/>

Command to create a Docker build:

```bash
oc new-build --strategy=docker --binary=true --name=web -l app=web centos/httpd-24-centos7
```

Start the build with the data from `labs/data/02_httpd`:

```bash
oc start-build web --from-dir=labs/data/02_httpd --follow
```

Follow how the build goes and if the image will be present in your registry.

Create an app with that image and expose it:

```bash
oc new-app web -l app=web
oc create route edge --service=web -l app=web
```

Now let's try to add an easter egg in /easter-egg.txt with a new build.
Inspect `labs/data/02_httpd` for a hint.

<details><summary>solution</summary>add a copy command to the Dockerfile to copy the file easter-egg.txt to /var/www/html/</details><br/>
