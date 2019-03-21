# Develop Application

In diesem Lab werden wir gemeinsam das erste "pre-built" Docker Image deployen und die OpenShift-Konzepte Pod, Service, DeploymentConfig und ImageStream etwas genauer anschauen.


## Aufgabe

Nachdem wir im vorher den Source-to-Image Workflow, wie auch ein Binary und Docker Build verwendet haben, um eine Applikation auf OpenShift zu deployen, wenden wir uns nun dem Deployen eines pre-built Docker Images von Docker Hub oder einer anderen Docker-Registry zu.

> [Weiterführende Dokumentation](https://docs.openshift.com/container-platform/3.11/dev_guide/application_lifecycle/new_app.html#specifying-an-image)

Als ersten Schritt erstellen wir dafür ein neues Projekt. Ein Projekt ist eine Gruppierung von Ressourcen (Container und Docker Images, Pods, Services, Routen, Konfiguration, Quotas, Limiten und weiteres). Für das Projekt berechtigte User können diese Ressourcen verwalten. Innerhalb eines OpenShift Clusters muss der Name eines Projektes eindeutig sein.

Erstellen Sie daher ein neues Projekt mit dem Namen `develop-userXY`:

```
$ oc new-project develop-userXY
```

`oc new-project` wechselt automatisch in das eben neu angelegte Projekt. Mit dem `oc get` Command können Ressourcen von einem bestimmten Typ angezeigt werden.

Verwenden Sie
```
$ oc get project
```
um alle Projekte anzuzeigen, auf die Sie berechtigt sind.

Sobald das neue Projekt erstellt wurde, können wir in OpenShift mit dem folgenden Befehl das Docker Image deployen:

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

Für unser Lab verwenden wir ein APPUiO-Beispiel (Java Spring Boot Applikation):
- Docker Hub: https://hub.docker.com/r/appuio/example-spring-boot/
- GitHub (Source): https://github.com/appuio/example-spring-boot-helloworld

OpenShift legt die nötigen Ressourcen an, lädt das Docker Image in diesem Fall von Docker Hub herunter und deployt anschliessend den ensprechenden Pod.

**Tipp:** Verwenden Sie `oc status` um sich einen Überblick über das Projekt zu verschaffen.

Oder verwenden Sie den `oc get` Befehl mit dem `-w` Parameter, um fortlaufend Änderungen an den Ressourcen des Typs Pod anzuzeigen (abbrechen mit ctrl+c):
```
$ oc get pods -w
```

Je nach Internetverbindung oder abhängig davon, ob das Image auf Ihrem OpenShift Node bereits heruntergeladen wurde, kann das eine Weile dauern. Schauen Sie sich doch in der Web Console den aktuellen Status des Deployments an:

1. Loggen Sie sich in der Web Console ein
2. Wählen Sie Ihr Projekt `develop-userXY` aus
3. Klicken Sie auf Applications
4. Wählen Sie Pods aus


**Tipp** Um Ihre eigenen Docker Images für OpenShift zu erstellen, sollten Sie die folgenden Best Practices befolgen: https://docs.openshift.com/container-platform/3.11/creating_images/guidelines.html


## Betrachten der erstellten Ressourcen

Als wir `oc new-app appuio/example-spring-boot` vorhin ausführten, hat OpenShift im Hintergrund einige Ressourcen für uns angelegt. Die werden dafür benötigt, dieses Docker Image zu deployen:

- [Service](https://docs.openshift.com/container-platform/3.11/architecture/core_concepts/pods_and_services.html#services)
- [ImageStream](https://docs.openshift.com/container-platform/3.11/architecture/core_concepts/builds_and_image_streams.html#image-streams)
- [DeploymentConfig](https://docs.openshift.com/container-platform/3.11/dev_guide/deployments/how_deployments_work.html)

### Service

[Services](https://docs.openshift.com/container-platform/3.11/architecture/core_concepts/pods_and_services.html#services) dienen innerhalb OpenShift als Abstraktionslayer, Einstiegspunkt und Proxy/Loadbalancer auf die dahinterliegenden Pods. Der Service ermöglicht es, innerhalb OpenShift eine Gruppe von Pods des gleichen Typs zu finden und anzusprechen.

Als Beispiel: Wenn eine Applikationsinstanz unseres Beispiels die Last nicht mehr alleine verarbeiten kann, können wir die Applikation bspw. auf drei Pods hochskalieren. OpenShift mapt diese als Endpoints automatisch zum Service. Sobald die Pods bereit sind, werden Requests automatisch auf alle drei Pods verteilt.

**Note:** Die Applikation kann aktuell von aussen noch nicht erreicht werden, der Service ist ein OpenShift-internes Konzept. Im folgenden Lab werden wir die Applikation öffentlich verfügbar machen.

Nun schauen wir uns unseren Service mal etwas genauer an:

```
$ oc get services
```

```
NAME                  CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
example-spring-boot   172.30.124.20   <none>        8080/TCP   2m
```

Wie Sie am Output sehen, ist unser Service (example-spring-boot) über eine IP und Port erreichbar (172.30.124.20:8080) **Note:** Ihre IP kann unterschiedlich sein.

**Note:** Service IPs bleiben während ihrer Lebensdauer immer gleich.

Mit dem folgenden Befehl können Sie zusätzliche Informationen über den Service auslesen:
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

Mit dem entsprechenden Befehl können Sie auch die Details zu einem Pod anzeigen:
```
$ oc get pod example-spring-boot-3-nwzku -o json
```

**Note:** Zuerst den pod Namen aus Ihrem Projekt abfragen (`oc get pods`) und im oberen Befehl ersetzen.

Über den `selector` Bereich im Service wird definiert, welche Pods (`labels`) als Endpoints dienen. Dazu können die entsprechenden Konfigurationen von Service und Pod zusammen betrachtet werden.

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

Diese Verknüpfung ist besser mittels `oc describe` Befehl zu sehen:
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

Unter Endpoints finden Sie nun den aktuell laufenden Pod.


### ImageStream
[ImageStreams](https://docs.openshift.com/container-platform/3.11/architecture/core_concepts/builds_and_image_streams.html#image-streams) werden dafür verwendet, automatische Tasks auszuführen wie bspw. ein Deployment zu aktualisieren, wenn eine neue Version des Images oder des Basisimages verfügbar ist.

Builds und Deployments können Image Streams beobachten und auf Änderungen entsprechend reagieren. In unserem Beispiel wird der Image Stream dafür verwendet, ein Deployment zu triggern, sobald etwas am Image geändert hat.

Mit dem folgenden Befehl können Sie zusätzliche Informationen über den Image Stream auslesen:
```
$ oc get imagestream example-spring-boot -o json
```

### DeploymentConfig

In der [DeploymentConfig](https://docs.openshift.com/container-platform/3.11/dev_guide/deployments/how_deployments_work.html) werden folgende Punkte definiert:

- Update Strategy: wie werden Applikationsupdates ausgeführt, wie erfolgt das Austauschen der Container?
- Triggers: Welche Triggers führen zu einem Deployment? In unserem Beispiel ImageChange
- Container
  - Welches Image soll deployed werden?
  - Environment Configuration für die Pods
  - ImagePullPolicy
- Replicas, Anzahl der Pods, die deployt werden sollen


Mit dem folgenden Befehl können zusätzliche Informationen zur DeploymentConfig ausgelesen werden:
```
$ oc get deploymentConfig example-spring-boot -o json
```

Im Gegensatz zur DeploymentConfig, mit welcher man OpenShift sagt, wie eine Applikation deployt werden soll, definiert man mit dem ReplicationController, wie die Applikation während der Laufzeit aussehen soll (bspw. dass immer 3 Replicas laufen sollen).

**Tipp:** für jeden Resource Type gibt es auch eine Kurzform. So können Sie bspw. `oc get deploymentconfig` auch einfach als `oc get dc` schreiben.

# Unseren Service mittels Route online verfügbar machen

In diesem Lab werden wir die Applikation von vorher über **https** vom Internet her erreichbar machen.

## Routen

Der `oc new-app` Befehl aus dem vorherigen [Lab](04_deploy_dockerimage.md) erstellt keine Route. Somit ist unser Service von *aussen* her gar nicht erreichbar. Will man einen Service verfügbar machen, muss dafür eine Route eingerichtet werden. Der OpenShift Router erkennt aufgrund des Host Headers auf welchen Service ein Request geleitet werden muss.

Aktuell werden folgende Protokolle unterstützt:

- HTTP
- HTTPS ([SNI](https://en.wikipedia.org/wiki/Server_Name_Indication))
- WebSockets
- TLS mit [SNI](https://en.wikipedia.org/wiki/Server_Name_Indication)

## Aufgabe

Vergewissern Sie sich, dass Sie sich im Projekt `develop-userXY` befinden. **Tipp:** `oc project develop-userXY`

Erstellen Sie für den Service `example-spring-boot` eine Route und machen Sie ihn darüber öffentlich verfügbar.

**Tipp:** Mittels `oc get routes` können Sie sich die Routen eines Projekts anzeigen lassen.

```
$ oc get routes
```

Aktuell gibt es noch keine Route. Jetzt brauchen wir den Servicenamen:

```
$ oc get services
NAME                  CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
example-spring-boot   172.30.124.20   <none>        8080/TCP   11m
```

Und nun wollen wir diesen Service veröffentlichen / exposen:

```
$ oc expose service example-spring-boot
```

Per default wird eine http Route erstellt.

Mittels `oc get routes` können wir überprüfen, ob die Route angelegt wurde.

```
$ oc get routes
NAME                  HOST/PORT                                   PATH      SERVICE                        TERMINATION   LABELS
example-spring-boot   example-spring-boot-techlab.mycluster.com             example-spring-boot:8080-tcp                 app=example-spring-boot
```

Die Applikation ist nun vom Internet her über den angegebenen Hostnamen erreichbar, Sie können also nun auf die Applikation zugreifen.

**Tipp:** Wird kein Hostname angegeben wird der Standardname verwendet: *servicename-project.osecluster*

In der Overview der Web Console ist diese Route mit dem Hostnamen jetzt auch sichtbar.


---

# Pod Scaling, Readiness Probe und Self Healing

In diesem Lab zeigen wir auf, wie man Applikationen in OpenShift skaliert. Des Weiteren zeigen wir, wie OpenShift dafür sorgt, dass jeweils die Anzahl erwarteter Pods gestartet wird und wie eine Applikation der Plattform zurückmelden kann, dass sie bereit für Requests ist.

## Example Applikation hochskalieren

Dafür erstellen wir ein neues Projekt

```
$ oc new-project scale-userXY
```

und fügen dem Projekt eine Applikation hinzu

```
$ oc new-app appuio/example-php-docker-helloworld --name=appuio-php-docker
```

und stellen den Service zur Verfügung (expose)

```
$ oc expose service appuio-php-docker
```

Wenn wir unsere Example Applikation skalieren wollen, müssen wir unserem ReplicationController (rc) mitteilen, dass wir bspw. stets 3 Replicas des Images am Laufen haben wollen.

Schauen wir uns mal den ReplicationController (rc) etwas genauer an:

```
$ oc get rc

NAME                  DESIRED   CURRENT   AGE
appuio-php-docker-1   1         1         33s
```

Für mehr Details:

```
$ oc get rc appuio-php-docker-1 -o json
```

Der rc sagt uns, wieviele Pods wir erwarten (spec) und wieviele aktuell deployt sind (status).

## Aufgabe: skalieren unserer Beispiel Applikation
Nun skalieren wir unsere Example Applikation auf 3 Replicas:

```
$ oc scale --replicas=3 dc appuio-php-docker
```

Überprüfen wir die Anzahl Replicas auf dem ReplicationController:

```
$ oc get rc

NAME                  DESIRED   CURRENT   AGE
appuio-php-docker-1   3         3         1m

```

und zeigen entsprechend die Pods an:

```
$ oc get pods
NAME                        READY     STATUS    RESTARTS   AGE
appuio-php-docker-1-2uc89   1/1       Running   0          21s
appuio-php-docker-1-evcre   1/1       Running   0          21s
appuio-php-docker-1-tolpx   1/1       Running   0          2m

```

Zum Schluss schauen wir uns den Service an. Der sollte jetzt alle drei Endpoints referenzieren:
```
$ oc describe svc appuio-php-docker
Name:      appuio-php-docker
Namespace:    techlab-scale
Labels:      app=appuio-php-docker
Selector:    app=appuio-php-docker,deploymentconfig=appuio-php-docker
Type:      ClusterIP
IP:        172.30.166.88
Port:      8080-tcp  8080/TCP
Endpoints:    10.1.3.23:8080,10.1.4.13:8080,10.1.5.15:8080
Session Affinity:  None
No events.

```

Skalieren von Pods innerhalb eines Services ist sehr schnell, da OpenShift einfach eine neue Instanz des Docker Images als Container startet.

**Tipp:** OpenShift unterstützt auch Autoscaling, die Dokumentation dazu ist unter dem folgenden Link zu finden: https://docs.openshift.com/container-platform/3.11/dev_guide/pod_autoscaling.html - Wir werden uns damit später noch detailierter beschäftigen.

## Aufgabe: skalierte App in der Web Console

Schauen Sie sich die skalierte Applikation auch in der Web Console an.

## Unterbruchsfreies Skalieren überprüfen

Mit dem folgenden Befehl können Sie nun überprüfen, ob Ihr Service verfügbar ist, während Sie hoch und runter skalieren.
Ersetzen Sie dafür `[route]` mit Ihrer definierten Route:

**Tipp:** oc get route

```
while true; do sleep 1; curl -s http://[route]/pod/; date "+ TIME: %H:%M:%S,%3N"; done
```

und skalieren Sie von **3** Replicas auf **1**.
Der Output zeigt jeweils den Pod an, der den Request verarbeitete:

```
POD: appuio-php-docker-6-9w9t4 TIME: 16:40:04,991
POD: appuio-php-docker-6-9w9t4 TIME: 16:40:06,053
POD: appuio-php-docker-6-6xg2b TIME: 16:40:07,091
POD: appuio-php-docker-6-6xg2b TIME: 16:40:08,128
POD: appuio-php-docker-6-ctbrs TIME: 16:40:09,175
POD: appuio-php-docker-6-ctbrs TIME: 16:40:10,212
POD: appuio-php-docker-6-9w9t4 TIME: 16:40:11,279
POD: appuio-php-docker-6-9w9t4 TIME: 16:40:12,332
POD: appuio-php-docker-6-6xg2b TIME: 16:40:13,369
POD: appuio-php-docker-6-6xg2b TIME: 16:40:14,407
POD: appuio-php-docker-6-6xg2b TIME: 16:40:15,441
POD: appuio-php-docker-6-6xg2b TIME: 16:40:16,493
POD: appuio-php-docker-6-6xg2b TIME: 16:40:17,543
POD: appuio-php-docker-6-6xg2b TIME: 16:40:18,591
```
Die Requests werden an die unterschiedlichen Pods geleitet, sobald man runterskaliert auf einen Pod, gibt dann nur noch einer Antwort

Was passiert nun, wenn wir nun während dem der While Befehl oben läuft, ein neues Deployment starten:

```
$ oc rollout latest appuio-php-docker
```
Währen einer kurzen Zeit gibt die öffentliche Route keine Antwort
```
POD: appuio-php-docker-6-6xg2b TIME: 16:42:17,743
POD: appuio-php-docker-6-6xg2b TIME: 16:42:18,776
POD: appuio-php-docker-6-6xg2b TIME: 16:42:19,813
POD: appuio-php-docker-6-6xg2b TIME: 16:42:20,853
POD: appuio-php-docker-6-6xg2b TIME: 16:42:21,891
POD: appuio-php-docker-6-6xg2b TIME: 16:42:22,943
POD: appuio-php-docker-6-6xg2b TIME: 16:42:23,980
# keine Antwort
POD: appuio-php-docker-7-pxnr3 TIME: 16:42:42,134
POD: appuio-php-docker-7-pxnr3 TIME: 16:42:43,181
POD: appuio-php-docker-7-pxnr3 TIME: 16:42:44,226
POD: appuio-php-docker-7-pxnr3 TIME: 16:42:45,259
POD: appuio-php-docker-7-pxnr3 TIME: 16:42:46,297
POD: appuio-php-docker-7-pxnr3 TIME: 16:42:47,571
POD: appuio-php-docker-7-pxnr3 TIME: 16:42:48,606
POD: appuio-php-docker-7-pxnr3 TIME: 16:42:49,645
POD: appuio-php-docker-7-pxnr3 TIME: 16:42:50,684
```

In unserem Beispiel verwenden wir einen sehr leichtgewichtigen Pod. Das Verhalten ist ausgeprägter, wenn der Container länger braucht bis er Requests abarbeiten kann. Bspw. Java Applikation.

```
Pod: example-spring-boot-2-73aln TIME: 16:48:25,251
Pod: example-spring-boot-2-73aln TIME: 16:48:26,305
Pod: example-spring-boot-2-73aln TIME: 16:48:27,400
Pod: example-spring-boot-2-73aln TIME: 16:48:28,463
Pod: example-spring-boot-2-73aln TIME: 16:48:29,507
<html><body><h1>503 Service Unavailable</h1>
No server is available to handle this request.
</body></html>
 TIME: 16:48:33,562
<html><body><h1>503 Service Unavailable</h1>
No server is available to handle this request.
</body></html>
 TIME: 16:48:34,601
 ...
Pod: example-spring-boot-3-tjdkj TIME: 16:49:20,114
Pod: example-spring-boot-3-tjdkj TIME: 16:49:21,181
Pod: example-spring-boot-3-tjdkj TIME: 16:49:22,231

```

Es kann dann sogar sein, dass der Service gar nicht mehr online ist und der Routing Layer ein **503 Error** zurück gibt.

Im Folgenden Kapitel wird beschrieben, wie Sie Ihre Services konfigurieren können, dass unterbruchsfreie Deployments möglich werden.


## Unterbruchsfreies Deployment mittels Readiness Probe und Rolling Update

Die Update Strategie [Rolling](https://docs.openshift.com/container-platform/3.11/dev_guide/deployments/deployment_strategies.html#rolling-strategy) ermöglicht unterbruchsfreie Deployments. Damit wird die neue Version der Applikation gestartet, sobald die Applikation bereit ist, werden Request auf den neuen Pod geleitet und die alte Version undeployed.

Zusätzlich kann mittels [Container Health Checks](https://docs.openshift.com/container-platform/3.11/dev_guide/application_health.html) die deployte Applikation der Plattform detailliertes Feedback über ihr aktuelles Befinden geben.

Grundsätzlich gibt es zwei Checks, die implementiert werden können:

- Liveness Probe, sagt aus, ob ein laufender Container immer noch sauber läuft.
- Readiness Probe, gibt Feedback darüber, ob eine Applikation bereit ist, um Requests zu empfangen. Ist v.a. im Rolling Update relevant.

Diese beiden Checks können als HTTP Check, Container Execution Check (Shell Script im Container) oder als TCP Socket Check implementiert werden.

In unserem Beispiel soll die Applikation der Plattform sagen, ob sie bereit für Requests ist. Dafür verwenden wir die Readiness Probe. Unsere Beispielapplikation gibt auf der folgenden URL auf Port 9000 (Management-Port der Spring Applikation) ein Status Code 200 zurück, sobald die Applikation bereit ist.
```
http://[route]/health/
```

## Aufgabe

In der Deployment Config (dc) definieren im Abschnitt der Rolling Update Strategie, dass bei einem Update die App immer verfügbar sein soll: `maxUnavailable: 0%`

Dies kann in der Deployment Config (dc) konfiguriert werden:

**YAML:**
```
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

Die Deployment Config kann via Web Console oder direkt über `oc` editiert werden.
```
$ oc edit dc appuio-php-docker
```

Oder im JSON-Format editieren:
```
$ oc edit dc appuio-php-docker -o json
```
**json**
```
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

Die Readiness Probe muss in der Deployment Config (dc) hinzugefügt werden, und zwar unter:

spec --> template --> spec --> containers unter halb von `resources: {  }`

**YAML:**

```
...
          resources: {  }
          readinessProbe:
            httpGet:
              path: /health/
              port: 8080
              scheme: HTTP
            initialDelaySeconds: 10
            timeoutSeconds: 1
...
```

**json:**
```
...
                        "resources": {},
                        "readinessProbe": {
                            "httpGet": {
                                "path": "/health/",
                                "port": 8080,
                                "scheme": "HTTP"
                            },
                            "initialDelaySeconds": 10,
                            "timeoutSeconds": 1
                        },
...
```

Passen Sie das entsprechend analog oben an.

Die Konfiguration unter Container muss dann wie folgt aussehen:
**YAML:**
```
      containers:
        -
          name: example-php-docker-helloworld
          image: 'appuio/example-php-docker-helloworld@sha256:6a19d4a1d868163a402709c02af548c80635797f77f25c0c391b9ce8cf9a56cf'
          ports:
            -
              containerPort: 8080
              protocol: TCP
          resources: {  }
          readinessProbe:
            httpGet:
              path: /health/
              port: 8080
              scheme: HTTP
            initialDelaySeconds: 10
            timeoutSeconds: 1
          terminationMessagePath: /dev/termination-log
          imagePullPolicy: IfNotPresent
```

**json:**
```
                "containers": [
                    {
                        "name": "appuio-php-docker",
                        "image": "appuio/example-php-docker-helloworld@sha256:9e927f9d6b453f6c58292cbe79f08f5e3db06ac8f0420e22bfd50c750898c455",
                        "ports": [
                            {
                                "containerPort": 8080,
                                "protocol": "TCP"
                            }
                        ],
                        "resources": {},
                        "readinessProbe": {
                            "httpGet": {
                                "path": "/health/",
                                "port": 8080,
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
```
while true; do sleep 1; curl -s http://[route]/pod/; date "+ TIME: %H:%M:%S,%3N"; done
```

Starten des Deployments:
```
$ oc rollout latest appuio-php-docker
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
oc delete pod appuio-php-docker-3-788j5
```

OpenShift sorgt dafür, dass wieder **n** Replicas des genannten Pods laufen.

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
$ oc process --parameters mysql-persistent
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
              "value": "jdbc:mysql://techlab/appuio"
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

