# Troubleshooting, was ist im Pod?

In diesem Lab wird aufgezeigt, wie man im Fehlerfall und Troubleshooting vorgehen kann und welche Tools einem dabei zur Verfügung stehen.

## In Container einloggen

Wir verwenden dafür wieder das Projekt `userXY-develop`.
<details><summary>Tipp</summary>oc project userXY-develop</details><br/>

Laufende Container werden als unveränderbare Infrastruktur behandelt und sollen generell nicht modifiziert werden. Dennoch gibt es Usecases, bei denen man sich in die Container einloggen muss. Zum Beispiel für Debugging und Analysen.

## Aufgabe

Mit OpenShift können Remote Shells in die Pods geöffnet werden ohne dass man darin vorgängig SSH installieren müsste. Dafür steht einem der Befehl `oc rsh` zur Verfügung.

Wählen Sie einen Pod aus und öffnen Sie die Remote Shell.
<details><summary>Tipp</summary>oc get pods<br/>oc rsh [POD]</details><br/>

Sie können nun über diese Shell Analysen im Container ausführen:

```bash
bash-4.2$ ls -la
total 16
drwxr-xr-x. 7 default root   99 May 16 13:35 .
drwxr-xr-x. 4 default root   54 May 16 13:36 ..
drwxr-xr-x. 6 default root   57 May 16 13:35 .gradle
drwxr-xr-x. 3 default root   18 May 16 12:26 .pki
drwxr-xr-x. 9 default root 4096 May 16 13:35 build
-rw-r--r--. 1 root    root 1145 May 16 13:33 build.gradle
drwxr-xr-x. 3 root    root   20 May 16 13:34 gradle
-rwxr-xr-x. 1 root    root 4971 May 16 13:33 gradlew
drwxr-xr-x. 4 root    root   28 May 16 13:34 src
```

## Aufgabe

Einzelne Befehle innerhalb des Containers können über `oc exec` ausgeführt werden:

```bash
oc exec [POD] env
```

```bash
$ oc exec example-spring-boot-4-8mbwe env
PATH=/opt/app-root/src/bin:/opt/app-root/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
HOSTNAME=example-spring-boot-4-8mbwe
KUBERNETES_SERVICE_PORT_DNS_TCP=53
KUBERNETES_PORT_443_TCP_PROTO=tcp
KUBERNETES_PORT_443_TCP_ADDR=172.30.0.1
KUBERNETES_PORT_53_UDP_PROTO=udp
KUBERNETES_PORT_53_TCP=tcp://172.30.0.1:53
...
```

## Logfiles anschauen

Die Logfiles zu einem Pod können sowohl in der Web Console als auch auch im CLI angezeigt werden.

```bash
oc logs [POD]
```

Der Parameter `-f` bewirkt analoges Verhalten wie `tail -f`

Befindet sich ein Pod im Status **CrashLoopBackOff** bedeutet dies, dass er auch nach wiederholtem Restarten nicht erfolgreich gestartet werden konnte. Die Logfiles können auch wenn der Pod nicht läuft mit dem folgenden Befehl angezeigt werden.

```bash
oc logs -p [POD]
```

Mit OpenShift wird ein EFK (Elasticsearch, Fluentd, Kibana) Stack mitgeliefert, der sämtliche Logfiles sammelt, rotiert und aggregiert. Kibana erlaubt es Logs zu durchsuchen, zu filtern und grafisch aufzubereiten.

Kibana ist über den Link "View Archive" im Web-UI bei den Logs des Pods erreichbar. Melde dich im Kibana an, schaue dich um und versuche eine Suche für bestimmte Logs zu definieren.

## Metriken

Die OpenShift Platform integriert auch ein Grundset an Metriken, welche einerseits im WebUI integriert werden und anderseits auch dazu genutzt werden um Pods automatisch horizontal zu skalieren.

Sie können mit Hilfe eines direkten Logins auf einen Pod nun den Ressourcenverbrauch dieses Pods beeinflussen und die Auswirkungen dazu im WebUI beobachten.

## Aufgabe: Port Forwarding

OpenShift 3 erlaubt es, beliebige Ports von der Entwicklungs-Workstation auf einen Pod weiterzuleiten. Dies ist z.B. nützlich, um auf Administrationskonsolen, Datenbanken, usw. zuzugreifen, die nicht gegen das Internet exponiert werden und auch sonst nicht erreichbar sind. Im Gegensatz zu OpenShift 2 werden die Portweiterleitungen über dieselbe HTTPS-Verbindung getunnelt, die der OpenShift Client (oc) auch sonst benutzt. Dies erlaubt es auch dann auf OpenShift 3 Platformen zuzugreifen, wenn sich restriktive Firewalls und/oder Proxies zwischen Workstation und OpenShift befinden.

Übung: Auf die Spring Boot Metrics zugreifen.

```
oc get pod --namespace="userXY-develop"
oc port-forward example-spring-boot-1-xj1df 9000:9000 --namespace="userXY-develop"
```

Nicht vergessen den Pod Namen an die eigene Installation anzupassen. Falls installiert kann dafür Autocompletion verwendet werden.

Die Metrics können nun unter folgendem Link abgerufen werden: [http://localhost:9000/metrics/](http://localhost:9000/metrics/) Die Metrics werden Ihnen als JSON angezeigt. Mit demselben Konzept können Sie nun bspw. mit Ihrem lokalen SQL Client auf eine Datenbank verbinden.

Unter folgendem Link sind weiterführende Informationen zu Port Forwarding zu finden: https://docs.openshift.com/container-platform/3.11/dev_guide/port_forwarding.html

**Note:** Der `oc port-forward`-Prozess wird solange weiterlaufen, bis er vom User abgebrochen wird. Sobald das Port-Forwarding also nicht mehr benötigt wird, kann er mit ctrl+c gestoppt werden.

## Aufgabe 2: Readyness check

In einer früheren Aufgabe haben wir für die Rolling update Strategie einen Readyness check auf einen /health eingerichtet. Dieser Endpoint war über die Route nicht erreichbar. Wie kann der endpoint nun erreicht werden?

## Autoscaling

In diesem Beispiel werden wir eine Applikation automatisierte hoch und runter skalieren, je nach dem unter wieviel Last die Applikation steht. Dazu verwenden wir eine Ruby example webapp.

Erstellen Sie daher ein neues Projekt mit dem Namen `userXY-autoscale`:
<details><summary>Tipp</summary>oc new-project userXY-autoscale</details><br/>

Auf dem Branch load gibt es einen CPU intensiven Endpunkt, welchen wir für unsere Tests verwenden werden. Dafür starten wir die App auf diesem Branch:

```bash
oc new-app openshift/ruby:2.5~https://github.com/chrira/ruby-ex.git#load
oc create route edge --insecure-policy=Redirect --service=ruby-ex
```

Warten sie bis die Applikation gebaut und ready ist und erste Metriken auftauchen. Sie können dem Build, wie auch den vorhandenden Pods folgen.

Bis die ersten Metriken auftauchen dauert es eine Weile, erst dann wird der Autoscaler richtig arbeiten können.

Nun definieren wir ein Set an Limiten für unsere Applikation, die für einen einzelnen Pod gültigkeit haben.
Dazu editieren wir die `ruby-ex` DeploymentConfiguration:
<details><summary>Tipp</summary>oc edit dc ruby-ex</details><br/>

Folgende Resource Limiten fügen wir dem Container hinzu:

```yaml
        resources:
          limits:
            cpu: "0.2"
            memory: "256Mi"
```

Die Ressourcen sind ursprünglich leer: `resources: {}`. Achtung die `resources` müssen auf dem Container und nicht dem Deployment definiert werden.

Dies wird unser Deployment neu ausrollen und die Limiten enforcen.

Sobald unser neuer Container läuft können wir nun den autoscaler konfigurieren:

```bash
oc autoscale dc ruby-ex --min 1 --max 3 --cpu-percent=25
```

Nun können wir auf dem Service Last erzeugen.

Ersetzen Sie dafür `[route]` mit Ihrer definierten Route:
<details><summary>Tipp</summary>oc get route</details><br/>

```bash
for i in {1..500}; do curl --insecure -s https://[route]/load ; done;
```

Die aktuellen Werte holen wir über:

```bash
oc get horizontalpodautoscaler.autoscaling/ruby-ex
```

Folgendermassen können wir unseren pods folgen:

```bash
oc get pods -w
```

Sobald wir die Last beenden wird die Anzahl Pods nach einer gewissen Zeit automatisch wieder herunter skaliert. Die Kapazität wird jedoch eine Weile vorenthalten.
