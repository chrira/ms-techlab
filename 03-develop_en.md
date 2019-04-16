# Develop Application

In this lab we will deploy the first "pre-built" Docker Image and take a closer look at the OpenShift concepts Pod, Service, DeploymentConfig and ImageStream.

## Aufgabe

After using the Source-to-Image workflow as well as a binary and docker build to deploy an application to OpenShift, we will now deploy a pre-built docker image from DockerHub or another docker registry.

> [Further Documentation](https://docs.openshift.com/container-platform/3.11/dev_guide/application_lifecycle/new_app.html#specifying-an-image)

As a first step we create a new project. A project is a grouping of resources (container and docker images, pods, services, routes, configuration, quotas, limits and more). Users authorized for the project can manage these resources. Within an OpenShift cluster, the name of a project must be unique.

Therefore, create a new project with the name `develop-userXY`:

```
$ oc new-project develop-userXY
```

`oc new-project` automatically changes to the newly created project. With `oc get` command, resources of a certain type can be displayed.

Use

```
$ oc get project
```

to list all the projects you have access to.

Once the new project has been created, we can deploy the Docker image in OpenShift with the following command:

```
$ oc new-app appuio/example-spring-boot
```

Output:

```
--> Found Docker image e355426 (3 months old) from Docker Hub for "appuio/example-spring-boot"

    APPUiO Spring Boot App
    ----------------------
    Example Spring Boot App

    Tags: builder, springboot

    * An image stream will be created as "example-spring-boot:latest" that will track this image
    * This image will be deployed in deployment config "example-spring-boot"
    * Port 8080/tcp will be load balanced by service "example-spring-boot"
      * Other containers can access this service through the hostname "example-spring-boot"

--> Creating resources with label app=example-spring-boot ...
    imagestream "example-spring-boot" created
    deploymentconfig "example-spring-boot" created
    service "example-spring-boot" created
--> Success
    Run 'oc status' to view your app.
```

For our lab we use an APPUiO example(Java Spring Boot Application):
- Docker Hub: https://hub.docker.com/r/appuio/example-spring-boot/
- GitHub (Source): https://github.com/appuio/example-spring-boot-helloworld

OpenShift creates the necessary resources, downloads the Docker image from Docker Hub and deploys the corresponding Pod.

**Tipp:** Use `oc status` to get an overview of current project.

Alternatively, use `oc get` command with `-w` Parameter, to see ongoing changes of resources with type pod (cancel with ctrl+c):
```
$ oc get pods -w
```

Depending on your internet connection or whether the image on your OpenShift Node has already been downloaded, this may take a while. Check the current status of the deployment in the Web Console:

1. Log in to the Web Console
2. Select your project `develop-userXY`.
3. Click on Applications
4. Select Pods

**Tip** To create your own Docker Images for OpenShift, you should follow these best practices: https://docs.openshift.com/container-platform/3.11/creating_images/guidelines.html

## Viewing the created resources

When we were running `oc new-app appuio/example-spring-boot` earlier, OpenShift created some resources for us in the background. They are needed to deploy this Docker image:

- [Service](https://docs.openshift.com/container-platform/3.11/architecture/core_concepts/pods_and_services.html#services)
- [ImageStream](https://docs.openshift.com/container-platform/3.11/architecture/core_concepts/builds_and_image_streams.html#image-streams)
- [DeploymentConfig](https://docs.openshift.com/container-platform/3.11/dev_guide/deployments/how_deployments_work.html)

### Service

[Services](https://docs.openshift.com/container-platform/3.11/architecture/core_concepts/pods_and_services.html#services) serve within OpenShift as an abstraction layer, entry point and proxy/load balancer to the underlying pods. The service makes it possible to find and address a group of pods of the same type within OpenShift.

As an example: If an application instance in our example can no longer handle the load alone, we can upscale the application to three pods, for example. OpenShift automatically maps these as endpoints to the service. As soon as the pods are ready, requests are automatically distributed to all three pods.

**Note:** The application cannot yet be reached from the outside, the service is an OpenShift internal concept. In the following lab we will make the application publicly available.

Now let's take a closer look at our service:

```
$ oc get services
```

```
NAME                  CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
example-spring-boot   172.30.124.20   <none>        8080/TCP   2m
```

As you can see from the output, our service (example-spring-boot) is reachable via an IP and port (172.30.124.20:8080) **Note:** Your IP can be different.

**Note:** Service IPs always remain the same during their lifetime.

You can use the following command to read additional information about the service:
```
$ oc get service example-spring-boot -o json
```

```
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "example-spring-boot",
        "namespace": "techlab",
        "selfLink": "/api/v1/namespaces/techlab/services/example-spring-boot",
        "uid": "b32d0197-347e-11e6-a2cd-525400f6ccbc",
        "resourceVersion": "17247237",
        "creationTimestamp": "2016-06-17T11:29:05Z",
        "labels": {
            "app": "example-spring-boot"
        },
        "annotations": {
            "openshift.io/generated-by": "OpenShiftNewApp"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "8080-tcp",
                "protocol": "TCP",
                "port": 8080,
                "targetPort": 8080
            }
        ],
        "selector": {
            "app": "example-spring-boot",
            "deploymentconfig": "example-spring-boot"
        },
        "portalIP": "172.30.124.20",
        "clusterIP": "172.30.124.20",
        "type": "ClusterIP",
        "sessionAffinity": "None"
    },
    "status": {
        "loadBalancer": {}
    }
}
```

You can also use the appropriate command to view the details of a Pod:
```
$ oc get pod example-spring-boot-3-nwzku -o json
```

**Note:** First get the pod name from your project (`oc get pods`) and replace it in the upper command.

The `selector` area in the service defines which pods (`labels`) serve as endpoints. The corresponding configurations of Service and Pod can be viewed together.

Service (`oc get service <Service Name>`):
```
...
"selector": {
    "app": "example-spring-boot",
    "deploymentconfig": "example-spring-boot"
},

...
```

Pod (`oc get pod <Pod Name>`):
```
...
"labels": {
    "app": "example-spring-boot",
    "deployment": "example-spring-boot-1",
    "deploymentconfig": "example-spring-boot"
},
...
```

This link is better seen with the `oc describe` command:
```
$ oc describe service example-spring-boot
```

```
Name:      example-spring-boot
Namespace:    techlab
Labels:      app=example-spring-boot
Selector:    app=example-spring-boot,deploymentconfig=example-spring-boot
Type:      ClusterIP
IP:        172.30.124.20
Port:      8080-tcp  8080/TCP
Endpoints:    10.1.3.20:8080
Session Affinity:  None
No events.
```

Under Endpoints you will now find the current Pod.


### ImageStream
[ImageStreams](https://docs.openshift.com/container-platform/3.11/architecture/core_concepts/builds_and_image_streams.html#image-streams) are used to perform automatic tasks such as updating a deployment when a new version of the image or base image is available.

Builds and deployments can monitor image streams and respond to changes accordingly. In our example, the image stream is used to trigger a deployment once something has changed the image.

With the following command you can get additional information about the image stream:
```
$ oc get imagestream example-spring-boot -o json
```

### DeploymentConfig

In the [DeploymentConfig](https://docs.openshift.com/container-platform/3.11/dev_guide/deployments/how_deployments_work.html) the following points are defined:

- Update Strategy: how are application updates executed, how are containers exchanged?
- Triggers: Which triggers lead to a deployment? In our example ImageChange
- container
  - What image should be deployed?
  - Environment Configuration for the Pods
  - ImagePullPolicy
- Replicas, number of pods to be deployed


The following command can be used to read additional information about DeploymentConfig:
```
$ oc get deploymentConfig example spring boat -o json
```

In contrast to DeploymentConfig, which tells OpenShift how an application should be deployed, the ReplicationController defines how the application should behave during runtime (e.g. that 3 replicas should always run).

**Tip:** for each resource type there is also a short form. For example, you can write `oc get deploymentconfig` as `oc get dc`.

# Make our service available online via route

In this lab we will make the application accessible from Internet via **https**.


## Routes

`oc new-app` command from the previous Lab does not create a route. So our service is not reachable from *outside* at all. If you want to make a service available, you have to set up a route for it. The OpenShift Router recognizes which service a request has to be routed to based on the host header.

Currently the following protocols are supported:

- HTTP
- HTTPS ([SNI](https://en.wikipedia.org/wiki/Server_Name_Indication))
- web sockets
- TLS with [SNI](https://en.wikipedia.org/wiki/Server_Name_Indication)

## Task

Make sure that you are in the project `develop-userXY`. **Tip:** `oc project develop-userXY`

Create a route for the `example-spring-boot` service and make it publicly available.

**Tip:** With `oc get routes` you can display the routes of a project.

```
$ oc get routes
```

Currently there is no route. Now we need the service name:

```
$ oc get services
NAME                  CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
example-spring-boot   172.30.124.20   <none>        8080/TCP   11m
```

And now we want to publish / expose this service:

```
oc create route edge --service=example-spring-boot
```

By default, an http route is created.

With `oc get routes` we can check if the route has been created.

```
$ oc get routes
NAME                  HOST/PORT                                   PATH      SERVICE                        TERMINATION   LABELS
example-spring-boot   example-spring-boot-techlab.mycluster.com             example-spring-boot:8080-tcp                 app=example-spring-boot
```

The application is now accessible from the Internet via the specified host name, so you can now access the application.

**Tip:** If no hostname is specified, the default name is used: *servicename-project.osecluster*

In the Web Console Overview, this route with the host name is now also visible.

---

# Pod Scaling, Readiness Probe and Self Healing

In this lab we show you how to scale applications in OpenShift. Furthermore, we show how OpenShift ensures that the number of expected pods is started and how an application can report back to the platform that it is ready for requests.

## Upscale Example Application

For this we use the previous project

```
$ oc project develop-userXY
```

If we want to scale our example application, we have to tell our replication controller (rc) that we always want 3 replicas of the image to work.

Let's take a closer look at the ReplicationController (rc):

```
$ oc get rc
NAME                    DESIRED   CURRENT   READY     AGE
example-spring-boot-1   1         1         1         33s
```

For more details:

```
oc get rc example-spring-boot-1 -o json
```

The rc tells us how many pods we expect (spec) and how many are currently deployed (status).

## Task: scale our example application
Now we scale our Example application to 3 replicas:

```
$ oc scale --replicas=3 dc example-spring-boot
```

Let's check the number of replicas on the ReplicationController:

```bash
$ oc get rc
NAME                    DESIRED   CURRENT   READY     AGE
example-spring-boot-4   3         3         3         16m
```

and display the pods accordingly:

```bash
$ oc get pods
NAME                          READY     STATUS    RESTARTS   AGE
example-spring-boot-4-fqh9n   1/1       Running   0          1m
example-spring-boot-4-tznqp   1/1       Running   0          16m
example-spring-boot-4-vdhqc   1/1       Running   0          1m
```

Finally, we take a look at the service. It should now reference all three endpoints:

```bash
$ oc describe svc example-spring-boot
Name:              example-spring-boot
Labels:            app=example-spring-boot
Selector:          app=example-spring-boot,deploymentconfig=example-spring-boot
Type:              ClusterIP
IP:                172.30.14.18
Port:              8080-tcp  8080/TCP
TargetPort:        8080/TCP
Endpoints:         10.129.6.30:8080,10.130.2.33:8080,10.131.2.17:8080
Port:              9000-tcp  9000/TCP
TargetPort:        9000/TCP
Endpoints:         10.129.6.30:9000,10.130.2.33:9000,10.131.2.17:9000
Session Affinity:  None
Events:            <none>
```

Scaling pods within a service is very fast because OpenShift simply starts a new instance of the Docker image as a container.

**Tip:** OpenShift also supports autoscaling, the documentation can be found at the following link: https://docs.openshift.com/container-platform/3.11/dev_guide/pod_autoscaling.html - We will deal with this in more detail later.

## Task: scaled app in the web console

Take a look at the scaled application in the Web Console.

## Check uninterrupted scaling

With the following command you can now check if your service is available as you scale up and down.
Replace `[route]` with your defined route:

**Tip:** oc get route

```bash
while true; do sleep 1; curl -s https://[route]/pod/; date "+ TIME: %H:%M:%S,%3N"; done
```

and scale from **3**** replicas to **1****.
The output shows the Pod that processed the request:

```bash
Pod: example-spring-boot-4-tznqp TIME: 15:07:51,162
Pod: example-spring-boot-4-vdhqc TIME: 15:07:52,516
Pod: example-spring-boot-4-fqh9n TIME: 15:07:53,904
Pod: example-spring-boot-4-tznqp TIME: 15:07:55,319
Pod: example-spring-boot-4-vdhqc TIME: 15:07:56,670
Pod: example-spring-boot-4-fqh9n TIME: 15:07:58,308
Pod: example-spring-boot-4-vdhqc TIME: 15:07:59,666
Pod: example-spring-boot-4-tznqp TIME: 15:08:01,032
Pod: example-spring-boot-4-tznqp TIME: 15:08:02,454
Pod: example-spring-boot-4-fqh9n TIME: 15:08:03,814
Pod: example-spring-boot-4-fqh9n TIME: 15:08:05,193
Pod: example-spring-boot-4-vdhqc TIME: 15:08:06,547
```

The requests will be forwarded to the different pods, as soon as you scale down to a pod, you will get only one response

What happens now when we start a new deployment while the While command is running above?

```
$ oc rollout latest example-spring-boot
```

For some time the public route gives no answer

```bash
Pod: example-spring-boot-5-rv9qs TIME: 16:13:44,938
Pod: example-spring-boot-5-rv9qs TIME: 16:13:46,258
Pod: example-spring-boot-5-rv9qs TIME: 16:13:47,567
Pod: example-spring-boot-5-rv9qs TIME: 16:13:48,875

<html>

...

  <body>
    <div>
      <h1>Application is not available</h1>

...

</html>
 TIME: 16:14:10,287
Pod: example-spring-boot-6-q99dq TIME: 16:14:11,825
Pod: example-spring-boot-6-q99dq TIME: 16:14:13,132
Pod: example-spring-boot-6-q99dq TIME: 16:14:14,428
Pod: example-spring-boot-6-q99dq TIME: 16:14:15,726
Pod: example-spring-boot-6-q99dq TIME: 16:14:17,064
Pod: example-spring-boot-6-q99dq TIME: 16:14:18,362
Pod: example-spring-boot-6-q99dq TIME: 16:14:19,655
```

It may even happen that the service is no longer online and the routing layer returns a **503 error**.

The following chapter describes how to configure your services to allow interruption-free deployments.

## Uninterrupted deployment using Readiness Probe and Rolling Update

The update strategy [Rolling](https://docs.openshift.com/container-platform/3.11/dev_guide/deployments/deployment_strategies.html#rolling-strategy) allows interruption-free deployments. This will launch the new version of the application, as soon as the application is ready, Request will be routed to the new Pod and the old version undeployed.

In addition, the deployed application can give the platform detailed feedback about its current state via [Container Health Checks](https://docs.openshift.com/container-platform/3.11/dev_guide/application_health.html).

Basically, there are two checks that can be implemented:

- Liveness Probe, indicates whether a running container is still running cleanly.
- Readiness Probe, gives feedback on whether an application is ready to receive requests. Is particularly relevant in the rolling update.

These two checks can be implemented as HTTP Check, Container Execution Check (Shell Script in Container) or TCP Socket Check.

In our example, the application of the platform should tell if it is ready for requests. For this we use the Readiness Probe. Our example application returns a status code 200 on the following URL on port 9000 (management port of the Spring application) as soon as the application is ready.

```bash
curl http://[route]/health/
```

## Task

In the Deployment Config (dc) section of the Rolling Update Strategy, define that the app should always be available during an update: `maxUnavailable: 0%`.

This can be configured in the Deployment Config (dc):

**YAML:**

```yaml
...
spec:
  strategy:
    type: Rolling
    rollingParams:
      updatePeriodSeconds: 1
      intervalSeconds: 1
      timeoutSeconds: 600
      maxUnavailable: 0%
      maxSurge: 25%
    resources: {  }
...
```

The Deployment Config can be edited via Web Console or directly via `oc`.
```
$ oc edit dc example-spring-boot
```

Or edit in JSON format:
```
$ oc edit dc example-spring-boot -o json
```

**json**

```json
"strategy": {
    "type": "Rolling",
    "rollingParams": {
          "updatePeriodSeconds": 1,
          "intervalSeconds": 1,
          "timeoutSeconds": 600,
          "maxUnavailable": "0%",
          "maxSurge": "25%"
    },
    "resources": {}
}
```

For the probes you need the Maintenance Port (9000).

To do this, add the port in the Deployment Config (dc) if it is not already there. This under:

spec --> template --> spec --> containers --> ports:

```yaml
...
        name: example-spring-boot
        ports:
...
        - containerPort: 9000
          protocol: TCP
...
```

Die Readiness Probe muss in der Deployment Config (dc) hinzugefügt werden, und zwar unter:

spec --> template --> spec --> spec --> Container unter halb von `Ressourcen: { }`

**YAML:**

```yaml
...
        resources: {  }
        readinessProbe:
          httpGet:
            path: /health
            port: 9000
            scheme: HTTP
          initialDelaySeconds: 10
          timeoutSeconds: 1
...
```

**json:**

```json
...
                        "resources": {},
                        "readinessProbe": {
                            "httpGet": {
                                "path": "/health",
                                "port": 9000,
                                "scheme": "HTTP"
                            },
                            "initialDelaySeconds": 10,
                            "timeoutSeconds": 1
                        },
...
```

Passen Sie das entsprechend analog oben an.

**Webconsole**

Die Readiness Probe kann auch in der Webconsole konfiguriert werden:

1. Application -> Deployments -> example-spring-boot
1. Oben rechts beim _Actions_ Button _Edit Health Checks_ auswählen: Add Readiness Probe
1. Port 9000 auswählen
1. Pfad: /health


Die Konfiguration unter Container muss dann wie folgt aussehen:

**YAML:**

```yaml
      containers:
      - image: appuio/example-spring-boot@sha256:f5336f4bdc3037269174b93f3731698216f1cc6276ea26b0429a137e943f1413
        imagePullPolicy: Always
        name: example-spring-boot
        ports:
            -
              containerPort: 8080
              protocol: TCP
              containerPort: 9000
              protocol: TCP
          resources: {  }
          readinessProbe:
            httpGet:
              path: /health
              port: 9000
              scheme: HTTP
            initialDelaySeconds: 10
            timeoutSeconds: 1
          terminationMessagePath: /dev/termination-log
          imagePullPolicy: IfNotPresent
```

**json:**

```json


                "containers": [
                    {
                        "image": "appuio/example-spring-boot@sha256:f5336f4bdc3037269174b93f3731698216f1cc6276ea26b0429a137e943f1413",
                        "imagePullPolicy": "Always",
                        "name": "example-spring-boot",
                        "ports": [
                            {
                                "containerPort": 8080,
                                "protocol": "TCP"
                            },
                            {
                                "containerPort": 9000,
                                "protocol": "TCP"
                            }
                        ],
                        "resources": {},
                        "readinessProbe": {
                            "httpGet": {
                                "path": "/health",
                                "port": 9000,
                                "scheme": "HTTP"
                            },
                            "initialDelaySeconds": 10,
                            "timeoutSeconds": 1
                        },
                        "terminationMessagePath": "/dev/termination-log",
                        "imagePullPolicy": "Always"
                    }
                ],
```

Verifizieren Sie während eines Deployment der Applikation, ob nun auch ein Update der Applikation unterbruchsfrei verläuft:

Einmal pro Sekunde ein Request:

```bash
while true; do sleep 1; curl -s http://[route]/pod/; date "+ TIME: %H:%M:%S,%3N"; done
```

Starten des Deployments:

```bash
$ oc rollout latest example-spring-boot
deploymentconfig.apps.openshift.io/example-spring-boot rolled out
```

## Self Healing

Über den Replication Controller haben wir nun der Plattform mitgeteilt, dass jeweils **n** Replicas laufen sollen. Was passiert nun, wenn wir einen Pod löschen?

Suchen Sie mittels `oc get pods` einen Pod im Status "running" aus, den Sie *killen* können.

Starten sie in einem eigenen Terminal den folgenden Befehl (anzeige der Änderungen an Pods)

```
oc get pods -w
```

Löschen Sie im anderen Terminal einen Pod mit folgendem Befehl

```
oc delete pod example-spring-boot-10-d8dkz
```

OpenShift sorgt dafür, dass wieder **n** Replicas des genannten Pods laufen.

In der Webconsole ist gut zu Beobachten, wie der Pod zuerst hellblau ist, bis die Applikation auf der Readiness Probe mit 0K antwortet.

# Datenbank anbinden

Die meisten Applikationen sind in irgend einer Art stateful und speichern Daten persistent ab. Sei dies in einer Datenbank oder als Files auf einem Filesystem oder Objectstore. In diesem Lab werden wir in unserem Projekt einen MySQL Service anlegen und an unsere Applikation anbinden, sodass mehrere Applikationspods auf die gleiche Datenbank zugreifen können.

Für dieses Beispiel verwenden wir das Spring Boot Beispiel `develop-userxy`. **Tipp:** `oc project develop-userxy`

## Aufgabe: MySQL Service anlegen

Für unser Beispiel verwenden wir in diesem Lab ein OpenShift Template, welches eine MySQL Datenbank mit EmptyDir Data Storage anlegt. Dies ist nur für Testumgebungen zu verwenden, da beim Restart des MySQL Pods alle Daten verloren gehen. In einem späteren Lab werden wir aufzeigen, wie wir ein Persistent Volume (mysql-persistent) an die MySQL Datenbank anhängen. Damit bleiben die Daten auch bei Restarts bestehen und ist so für den produktiven Betrieb geeignet.

Den MySQL Service können wir sowohl über die Web Console als auch über das CLI anlegen.

Um dasselbe Ergebnis zu erhalten müssen lediglich Datenbankname, Username, Password und DatabaseServiceName gleich gesetzt werden, egal welche Variante verwendet wird:

- MYSQL_USER techlab
- MYSQL_PASSWORD techlab
- MYSQL_DATABASE techlab
- DATABASE_SERVICE_NAME mysql

### CLI

Über das CLI kann der MySQL Service wie folgt mit Hilfe eines templates angelegt werden:

```
$ oc get templates
$ oc get templates -n openshift
$ oc process --parameters mysql-persistent -n openshift
$ oc get -n openshift template mysql-persistent -o yaml > mysql-persistent.yml
$ oc process -pMYSQL_USER=techlab -pMYSQL_PASSWORD=techlab -pMYSQL_DATABASE=techlab -f mysql-persistent.yml | oc create -f -
```

### Web Console

In der Web Console kann der MySQL (Ephemeral) Service via Catalog dem Projekt hinzugefügt werden. Dazu oben rechts auf *Add to Project*, *Browse Catalog* klicken und anschliessend unter dem Reiter *Databases* *MySQL* und *MySQL (Ephemeral)* auswählen.

### Passwort und Username als Plaintext?

Beim Deployen der Datebank via CLI wie auch via Web Console haben wir mittels Parameter Werte für User, Passwort und Datenbank angegeben. In diesem Kapitel wollen wir uns nun anschauen, wo diese sensitiven Daten effektiv gelandet sind.

Schauen wir uns als erstes die DeploymentConfig der Datenbank an:

```bash
$ oc get dc mysql -o yaml
```

Konkret geht es um die Konfiguration der Container mittels env (MYSQL_USER, MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD, MYSQL_DATABASE) in der DeploymentConfig unter `spec.templates.spec.containers`:

```yaml
    spec:
      containers:
      - env:
        - name: MYSQL_USER
          valueFrom:
            secretKeyRef:
              key: database-user
              name: mysql
        - name: MYSQL_PASSWORD
          valueFrom:
            secretKeyRef:
              key: database-password
              name: mysql
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              key: database-root-password
              name: mysql
        - name: MYSQL_DATABASE
          valueFrom:
            secretKeyRef:
              key: database-name
              name: mysql
```

Die Werte für die einzelnen Umgebungsvariablen kommen also aus einem sogenannten Secret, in unserem Fall hier aus dem Secret mit Namen `mysql`. In diesem Secret sind die vier Werte entsprechend unter den passenden Keys (`database-user`, `database-password`, `database-root-password`, `database-name`) abgelegt und können so referenziert werden.

Schauen wir uns nun die neue Ressource Secret mit dem Namen `mysql` an:

```bash
$ oc get secret mysql -o yaml
```

Die entsprechenden Key-Value Pairs sind unter `data` ersichtlich:

```yaml
apiVersion: v1
data:
  database-name: 
  database-password: YXBwdWlv
  database-root-password: dDB3ZDFLRFhsVjhKMGFHQw==
  database-user: YXBwdWlv
kind: Secret
metadata:
  annotations:
    openshift.io/generated-by: OpenShiftNewApp
    template.openshift.io/expose-database_name: '{.data[''database-name'']}'
    template.openshift.io/expose-password: '{.data[''database-password'']}'
    template.openshift.io/expose-root_password: '{.data[''database-root-password'']}'
    template.openshift.io/expose-username: '{.data[''database-user'']}'
  creationTimestamp: 2018-12-04T10:33:43Z
  labels:
    app: mysql-ephemeral
    template: mysql-ephemeral-template
  name: mysql
  ...
type: Opaque
```

Die konkreten Werte sind base64-kodiert. Unter Linux oder in der Gitbash kann man sich den entsprechenden Wert einfach mittels:

```bash
$ echo "dGVjaGxhYg==" | base64 -d
techlab
```
anzeigen lassen. In userem Fall wird `dGVjaGxhYg==` in `techlab` dekodiert.

Mit Secrets können wir also sensitive Informationen (Credetials, Zertifikate, Schlüssel, dockercfg, ...) abspeichern und entsprechend von den Pods entkoppeln. Gleichzeitig haben wir damit die Möglichkeit, dieselben Secrets in mehreren Containern zu verwenden und so Redundanzen zu vermeiden.

Secrets können entweder, wie oben bei der MySQL-Datenbank, in Umgebungsvariablen gemappt oder direkt als Files via Volumes in einen Container gemountet werden.

Weitere Informationen zu Secrets können in der [offiziellen Dokumentation](https://docs.openshift.com/container-platform/3.11/dev_guide/secrets.html) gefunden werden.

## Aufgabe: Applikation an die Datenbank anbinden

Standardmässig wird bei unserer example-spring-boot Applikation eine H2 Memory Datenbank verwendet. Dies kann über das Setzen der folgenden Umgebungsvariablen entsprechend auf unseren neuen MySQL Service umgestellt werden:

- SPRING_DATASOURCE_USERNAME techlab
- SPRING_DATASOURCE_PASSWORD techlab
- SPRING_DATASOURCE_DRIVER_CLASS_NAME com.mysql.jdbc.Driver
- SPRING_DATASOURCE_URL jdbc:mysql://[Adresse des MySQL Service]/techlab?autoReconnect=true

Für die Adresse des MySQL Service können wir entweder dessen Cluster IP (`oc get service`) oder aber dessen DNS-Namen (`<service>`) verwenden. Alle Services und Pods innerhalb eines Projektes können über DNS aufgelöst werden.

So lautet der Wert für die Variable SPRING_DATASOURCE_URL bspw.:
```
Name des Services: mysql

jdbc:mysql://mysql/techlab?autoReconnect=true
```

Diese Umgebungsvariablen können wir nun in der DeploymentConfig example-spring-boot setzen. Nach dem **ConfigChange** (ConfigChange ist in der DeploymentConfig als Trigger registriert) wird die Applikation automatisch neu deployed. Aufgrund der neuen Umgebungsvariablen verbindet die Applikation an die MySQL DB und [Liquibase](http://www.liquibase.org/) kreiert das Schema und importiert die Testdaten.

**Note:** Liquibase ist Open Source. Es ist eine Datenbank unabhängige Library um Datenbank Änderungen zu verwalten und auf der Datenbank anzuwenden. Liquibase erkennt beim Startup der Applikation, ob DB Changes auf der Datenbank angewendet werden müssen oder nicht. Siehe Logs.


```
SPRING_DATASOURCE_URL=jdbc:mysql://mysql/techlab?autoReconnect=true
```
**Note:** mysql löst innerhalb Ihres Projektes via DNS Abfrage auf die Cluster IP des MySQL Service auf. Die MySQL Datenbank ist nur innerhalb des Projektes erreichbar. Der Service ist ebenfalls über den folgenden Namen erreichbar:

```
Projektname = techlab-dockerimage

mysql.techlab-dockerimage.svc.cluster.local
```

Befehl für das Setzen der Umgebungsvariablen:
```
$ oc set env dc example-spring-boot \
      -e SPRING_DATASOURCE_URL="jdbc:mysql://mysql/techlab?autoReconnect=true" \
      -e SPRING_DATASOURCE_USERNAME=techlab \
      -e SPRING_DATASOURCE_PASSWORD=techlab \
      -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.jdbc.Driver
```

Über den folgenden Befehl können Sie sich die DeploymentConfig als JSON anschauen. Neu enthält die Config auch die gesetzten Umgebungsvariablen:

```
 $ oc get dc example-spring-boot -o json
```

```
...
 "env": [
          {
              "name": "SPRING_DATASOURCE_USERNAME",
              "value": "techlab"
          },
          {
              "name": "SPRING_DATASOURCE_PASSWORD",
              "value": "techlab"
          },
          {
              "name": "SPRING_DATASOURCE_DRIVER_CLASS_NAME",
              "value": "com.mysql.jdbc.Driver"
          },
          {
              "name": "SPRING_DATASOURCE_URL",
              "value": "jdbc:mysql://mysql/techlab?autoReconnect=true"
          }
      ],
...
```

Die Konfiguration kann auch in der Web Console angeschaut und verändert werden:

(Applications → Deployments → example-spring-boot, Actions, Edit YAML)

## Aufgabe: Secret referenzieren

Weiter oben haben wir gesehen, wie OpenShift mittels Secrets sensitive Informationen von der eigentlichen Konfiguration enkoppelt und uns dabei hilft, Redundanzen zu vermeiden. Unsere Springboot Applikation aus dem vorherigen Lab haben wir zwar korrekt konfiguriert, allerings aber die Werte redundant und Plaintext in der DeploymentConfig abgelegt.

Passen wir nun die DeploymentConfig example-spring-boot so an, dass die Werte aus den Secrets verwendet werden. Zu beachten gibt es die Konfiguration der Container unter `spec.template.spec.containers`

Mittels `oc edit dc example-spring-boot -o json` kann die DeploymentConfig als Json wie folgt bearbeitet werden.
```
...
"env": [
      {
        "name": "SPRING_DATASOURCE_USERNAME",
        "valueFrom": {
          "secretKeyRef": {
            "key": "database-user",
            "name": "mysql"
          }
        }
      },
      {
        "name": "SPRING_DATASOURCE_PASSWORD",
        "valueFrom": {
          "secretKeyRef": {
            "key": "database-password",
            "name": "mysql"
          }
        }
      },
      {
              "name": "SPRING_DATASOURCE_DRIVER_CLASS_NAME",
              "value": "com.mysql.jdbc.Driver"
          },
          {
              "name": "SPRING_DATASOURCE_URL",
              "value": "jdbc:mysql://mysql/techlab"
          }
    ],

...
```

Nun werden die Werte für Usernamen und Passwort sowohl beim mysql Pod wie auch beim Springboot Pod aus dem selben Secret gelesen.


## Aufgabe: In MySQL Service Pod einloggen und manuell auf DB verbinden

Es kann mittels `oc rsh [POD]` in einen Pod eingeloggt werden:
```
$ oc get pods
NAME                           READY     STATUS             RESTARTS   AGE
example-spring-boot-8-wkros    1/1       Running            0          10m
mysql-1-diccy                  1/1       Running            0          50m

```

Danach in den MySQL Pod einloggen:
```
$ oc rsh mysql-1-diccy
```

Nun können Sie mittels mysql Tool auf die Datenbank verbinden und die Tabellen anzeigen:
```
$ mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_SERVICE_HOST techlab
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 54
Server version: 5.6.26 MySQL Community Server (GPL)

Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql>
```

Anschliessend können Sie mit
```
show tables;
```

alle Tabellen anzeigen.


## Aufgabe: Dump auf MySQL DB einspielen

Die Aufgabe ist es, in den MySQL Pod den [Dump](https://raw.githubusercontent.com/appuio/techlab/lab-3.3/labs/data/08_dump/dump.sql) einzuspielen.


**Tipp:** Mit `oc rsync` können Sie lokale Dateien in einen Pod kopieren. Alternativ kann auch curl im mysql container verwendet werden.

**Achtung:** Beachten Sie, dass dabei der rsync-Befehl des Betriebssystems verwendet wird. Auf UNIX-Systemen kann rsync mit dem Paketmanager, auf Windows kann bspw. [cwRsync](https://www.itefix.net/cwrsync) installiert werden. Ist eine Installation von rsync nicht möglich, kann stattdessen bspw. in den Pod eingeloggt und via `curl -O <URL>` der Dump heruntergeladen werden.

**Tipp:** Verwenden Sie das Tool mysql um den Dump einzuspielen.

**Tipp:** Die bestehende Datenbank muss vorgängig leer sein. Sie kann auch gelöscht und neu angelegt werden.


---

## Lösung

Ein ganzes Verzeichnis (dump) syncen. Darin enthalten ist das File `dump.sql`. Beachten Sie zum rsync-Befehl auch obenstehenden Tipp sowie den fehlenden trailing slash.
```
oc rsync ./labs/data/08_dump mysql-1-diccy:/tmp/
```
In den MySQL Pod einloggen:

```
$ oc rsh mysql-1-diccy
```

Bestehende Datenbank löschen:
```
$ mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_SERVICE_HOST techlab
...
mysql> drop database techlab;
mysql> create database techlab;
mysql> exit
```
Dump einspielen:
```
$ mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -h$MYSQL_SERVICE_HOST techlab < /tmp/08_dump/dump.sql
```

**Note:** Den Dump kann man wie folgt erstellen:

```
mysqldump --user=$MYSQL_USER --password=$MYSQL_PASSWORD --host=$MYSQL_SERVICE_HOST techlab > /tmp/dump.sql
```
# Bonus - Integration Webhook

Die initiale ruby-ex Applikation ist auch in gogs gehostet. Machen sie einen Fork von der Applikation und integrieren sie den Webhook des Builds in das Projekt.

Wenn du nun Änderungen am Code durchführst, dann wird ein Build angestosen und die neue Version wird verfügbar.

