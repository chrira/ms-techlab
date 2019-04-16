# Troubleshooting, what's in the pod?

This lab shows how to proceed in case of errors and troubleshooting and which tools are available.

## Log in to container

We use the project `develop-userxy` again. **Tip:** `oc project develop-userxy`

Running containers are treated as unchangeable infrastructure and should generally not be modified. However, there are use cases where you have to log in to the containers. For example for debugging and analysis.

## Task

With OpenShift you can open remote shells in the pods without having to install SSH. You can use the `oc rsh` command to do this.

Select a Pod with `oc get pods` and execute the following command:
```
$ oc rsh [POD]
```

You can now use this shell to perform analyses in the container:

```
bash-4.2$ ls -la
total 16
drwxr-xr-x. 7 default root 99 May 16 13:35 .
drwxr-xr-x. 4 default root 54 May 16 13:36 ...
drwxr-xr-x. 6 default root 57 May 16 13:35 .gradle
drwxr-xr-x. 3 default root 18 May 16 12:26 .pki
drwxr-xr-x. 9 default root 4096 May 16 13:35 build
-rw-r--r--. 1 root root 1145 May 16 13:33 build.gradle
drwxr-xr-x. 3 root root 20 May 16 13:34 gradle
-rwxr-xr-x. 1 root root 4971 May 16 13:33 gradlew
drwxr-xr-x. 4 root root 28 May 16 13:34 src
```

## Task

Single commands within the container can be executed via `oc exec`:

```
$ oc exec [POD] env
```


```bash
$ oc exec example-spring-boot-4-8mbwe env
PATH=/opt/app-root/src/bin:/opt/app-root/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
HOSTNAME=example-spring-boot-4-8mbwe
CUBERNET_SERVICE_PORT_DNS_TCP=53
KUBERNETES_PORT_443_TCP_PROTO=tcp
KUBERNETES_PORT_443_TCP_ADDR=172.30.0.1
KUBERNETES_PORT_53_UDP_PROTO=udp
KUBERNETES_PORT_53_TCP=tcp://172.30.0.1:53
...
```

## view logfiles

The log files for a Pod can be displayed both in the Web Console and in the CLI.

```
$ oc logs [POD]
```

The parameter `-f` causes analogue behaviour like `tail -f`.

If a Pod has the status **CrashLoopBackOff** this means that it could not be started successfully even after repeated restarts. The log files can be displayed even if the Pod is not running with the following command.

 ```
$ oc logs -p [POD]
```

OpenShift comes with an EFK (Elasticsearch, Fluentd, Kibana) stack that collects, rotates and aggregates all log files. Kibana allows you to search, filter and graph logs.

Kibana can be reached via the link "View Archive" in the Web-UI at the logs of the Pod. Log in to Kibana, look around and try to define a search for specific logs.

## Metrics

The OpenShift Platform also integrates a basic set of metrics, which are integrated into the WebUI and used to automatically scale pods horizontally.

With the help of a direct login to a Pod you can now influence the resource consumption of this Pod and observe the effects in the WebUI.

## Task: Port Forwarding

OpenShift 3 allows you to forward arbitrary ports from the development workstation to a Pod. This is useful to access administration consoles, databases, etc. that are not exposed to the Internet and are not reachable otherwise. Port forwarding is tunnelled over the same HTTPS connection that the OpenShift client (oc) uses. This allows access to OpenShift 3 platforms even if there are restrictive firewalls and/or proxies between workstation and OpenShift.

Lab: Accessing the Spring Boot Metrics.

```
oc get pod --namespace="develop-userxy"
oc port-forward example-spring-boot-1-xj1df 9000:9000 --namespace="develop-userxy"
```

Don't forget to adapt the Pod name to your own installation. If installed, autocompletion can be used.

The metrics can now be accessed via the following link: [http://localhost:9000/metrics/](http://localhost:9000/metrics/) The Metrics are displayed as JSON. Using the same concept, you can now connect to a database with your local SQL client, for example.

Further information about Port Forwarding can be found under the following link: https://docs.openshift.com/container-platform/3.11/dev_guide/port_forwarding.html

**Note:** The `oc port-forward` process will continue until it is aborted by the user. As soon as the port forwarding is no longer needed, it can be stopped with ctrl+c.

## Task 2: Readyness check

In an earlier task, we set up a readiness check on one /health for the Rolling update strategy. This endpoint could not be reached via the route. How can the endpoint be reached now?

## Autoscaling

In this example we will scale an automated application up and down, depending on how much load the application is under. For this we use our old Ruby example webapp.

```
oc new-project autoscale-userXY
```

On the branch load there is a CPU intensive endpoint which we will use for our tests. Therefore we start the app on this branch:

```bash
oc new-app openshift/ruby:2.5~http://gogs.apps.zurich-XYZ.opensfhiftworkshop.com/ocpadmin/ruby-ex.git#load
oc create route edge --insecure-policy=Redirect --service=ruby-ex
```

Wait until the application is built and ready and the first metrics appear. You can follow the build as well as the existing pods.

It will take a while until the first metrics appear, then the autoscaler will be able to work properly.

Now we define a set of limits for our application that are valid for a single Pod:

```bash
oc edit dc ruby-ex
```

We add the following resource limits to the container:

```
        resources:
          limits:
            cpu: "0.2"
            memory: "256Mi"
```

The resources are originally empty: `resources: {}`. Attention the `resources` must be defined on the container and not on the deployment.

This will roll out our deployment again and enforce the limits.

As soon as our new container is running we can now configure the autoscaler:

```bash
oc autoscale dc ruby-ex --min 1 --max 3 --cpu-percent=25
```

Now we can generate load on the service:

```bash
for i in {1..500}; do curl -s https://ruby-ex-autoscale-userXY.apps.zurich-XYZ.openshiftworkshop.com/load ; done;
```

The current values we can get over:

```bash
oc get horizontalpodautoscaler.autoscaling/ruby-ex
```
Below we can follow our pods:

```bash
oc get pods -w
```

to retrieve. As soon as we finish the load the number of pods will be scaled down automatically after a certain time. However, the capacity is withheld for a while.

## Bonus

There is also a `oc idle`command. What is it for?
