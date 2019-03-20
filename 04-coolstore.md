# Coolstore

Deploy your version of coolstore

```
oc new-project coolstore-userXY
oc process -f https://raw.githubusercontent.com/jbossdemocentral/coolstore-microservice/stable-ocp-3.11/openshift/coolstore-template.yaml | oc create -f -
oc status
```

Wait till all images are built and all pods are deployed.

Familiarize yourself with deployed environment. List services and visit the cool store app.

Note: not all services are running, what happens if you start more of the services.

Note: You can test the individual apps from within the cluster:

```
oc rsh $(oc get pods -o name -l app=coolstore-gw)
curl http://catalog:8080/api/products
curl http://inventory:8080/api/availability/329299
curl http://cart:8080/api/cart/FOO
curl http://rating:8080/api/rating/329299
curl http://review:8080/api/review/329299
```
