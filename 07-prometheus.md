# App monitoring with prometheus

## Create a new project

```bash
oc new-project prometheus-userXY
```

## deploy example app

We will deploy our familiar ruby-ex application on a dedication branch called `prom`.

Create a new app.

```bash
oc new-app openshift/ruby:2.5~http://gogs.apps.six-zh-522a.openshiftworkshop.com/ocpadmin/ruby-ex.git#prom --name prometheus-app -l app=prometheus-app
```

Expose the service as route

```bash
oc create route edge --service=prometheus-app
```

## deploy prometheus

Create the prometheus instance

```bash
oc new-app -f data/prometheus/prometheus-template.yaml -p NAMESPACE=$(oc project --short)
```

Use route with name _prometheus_ to see the Prometheus UI.
You will need to log in with your OpenShift user.

### Graph tab

Select as an example the metric for `process_cpu_seconds_total`

Click on Execute and see the Graph and Console. This shows you

### Status tab

There you can inspect the lots of informations like flags, configuration, rules an so on.

On the Targets view, there should be a list of all service endpoints of Prometheus itself. Some are failing, though for this excerise this can be ignored.

Check with the port numbers which services do not expose their metrics.

## configure app to be scraped

Extend deployment configuration of the prometheus-app with prometheus endpoint informations.

```yaml
spec
...
  template:
    metadata:
      annotations:
        prometheus.io/path: /metrics
        prometheus.io/port: "8080"
        prometheus.io/scrape: "true"
```

Check the changes on the targets view of Prometheus (Status -> Targets):
https://prometheus-prometheus-userXY.apps.zurich-fdbf.openshiftworkshop.com/targets

Is the prometheus-app pod visible? Is it UP?

Note: It can take some time until the pod is listed.

## get custom metrics

Go inside the Prometheus Graph tab to see the custom metrics of the prometheus-app.

Select the metric for `http_server_requests_total`, this shows you all metrics with that type. You can also restrict to only get metrics from the prometheus-app: `http_server_requests_total{app="prometheus-app"}`

Click on Execute and see the Graph and Console.

Visit the app and see how your access is counted within prometheus.

Now let's generate some load. The following command, will generate every once in a while a call to /not-found, while otherwise calling /, so we should get some 404 metrics:

```bash
while true; do url="https://prometheus-app-prometheus-userXY.apps.six-zh-522a.openshiftworkshop.com/$(if [ $(( ( RANDOM % 10 )  + 1 )) -eq 10 ]; then echo "not-found"; fi)"; echo $url; curl -k -o /dev/null -s $url; sleep 0.25; done
```

We can now see how many http-codes we get on average over the last 5 minutes:

```
sum(rate(http_server_requests_total{app="prometheus-app"}[5m])) by (code)
```

This can also be grouped additionally by path:

```
sum(rate(http_server_requests_total{app="prometheus-app"}[5m])) by (code,path)
```


## Scale up

Scale the prometheus-app up to multiple pods.

Do you find the new pods inside Prometheus? Can you graph by return code and pod, so you can figure out which pods has the main issues?

## Bonus

Remember the spring boot app also had metrics available. Can you deploy this app again in our prometheus project and scrape this endpoint as well?
