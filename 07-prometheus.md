# App monitoring with prometheus

## Create a new project

```bash
oc new-project prometheus-userXY
```

## deploy example app

Clone the techlab Git repository, if you do not have it already available.

```
git clone http://gogs.apps.0xshift.dev/ocpadmin/techlab.git
```

Navigate to the root directory of the git repository from gogs (`cd techlab`).

Create a new app.

```bash
oc new-app fabric8/s2i-java~data/prometheus-app --name prometheus-app -l app=prometheus-app
```

The first build will fail because of the missing binary input.

start build:

```bash
oc start-build prometheus-app --from-dir=data/prometheus-app --follow
```

Expose the service as route

```bash
oc create route edge --service=prometheus-app
```

## deploy prometheus

Create the prometheus instance

```bash
oc new-app -f data/prometheus/prometheus3.7_without_clusterrole.yaml -p NAMESPACE=$(oc project --short)
```

Use route with name _prometheus_ to see the Prometheus UI.
You will need to log in with your OpenShift user.

### Graph tab

Select as an example the metric for _process_cpu_seconds_total_

Click on Execute and see the Graph and Console.

### Status tab

There you can inspect the lots of informations like flags, configuration, rules an so on.

On the Targets view, there should be a list of all service endpoints of Prometheus itself.

Check with the port numbers which services do not expose their metrics.

## configure app to be scraped

Extend deployment configuration of the prometheus-app with prometheus endpoint informations.

```yaml
spec
...
  template:
    metadata:
      annotations:
        prometheus.io/path: /actuator/prometheus
        prometheus.io/port: "8080"
        prometheus.io/scrape: "true"
```

Check the changes on the targets view of Prometheus (Status -> Targets):
https://prometheus-prometheus-userXY.apps.0xshift.dev/targets

Is the prometheus-app pod visible? Is it UP?

Note: It can take some time until the pod is listed.

## configure service to be scraped

Extend service configuration of the prometheus-app service.

```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/scheme: http
    prometheus.io/port: "8080"
```

### Reload Prometheus config

Kill Prometheus pod. It will reload the configuration.

```bash
oc delete pod prom-0 --grace-period=0 --force
```

saver way for prod:

```bash
oc exec prom-0 -c prometheus -- curl -X POST http://localhost:9090/-/reload
```