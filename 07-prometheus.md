# App monitoring with prometheus

## Create a new project

```bash
oc new-project prometheus-userXY
```

## deploy example app

Navigate to the root directory of the git repository from gogs.

Create a new app.

```bash
oc new-app redhat-openjdk18-openshift~data/prometheus-app --name prometheus-app -l app=prometheus-app
```

start build:

```bash
oc start-build prometheus-app --from-dir=data/prometheus-app --follow
```

Expose the service as route

```bash
oc create route edge --service=prometheus-app
```

## deploy prometheus

Create the prom secret

```bash
oc create secret generic prom --from-file=data/prometheus/prometheus.yml
 ```

Create the prom-alerts secret

```bash
oc create secret generic prom-alerts --from-file=data/prometheus/alertmanager.yml
 ```

Create the prometheus instance

```bash
oc process -f data/prometheus/prometheus-standalone.yaml | oc apply -f -
```

## configure app to be scraped

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

### Reload Prometheus config

Kill Prometheus pod. It will reload the configuration.

```bash
oc delete pod prom-0 --grace-period=0 --force
```

saver way for prod:

```bash
oc exec prom-0 -c prometheus -- curl -X POST http://localhost:9090/-/reload
```