# Setup

Notes for setup

## Users

On bastion host:

    ansible masters -m shell -a 'for i in `seq 1 25`; do htpasswd -b /etc/origin/master/htpasswd user$i 0p3nSh1ftuser$i; done'
    ansible masters[0] -m shell -a 'oc adm groups new techlab; for i in `seq 1 25`; do oc adm groups add-users techlab user$i; done'

## Gogs

### install

```bash
oc login -u ocpadmin https://api.0xshift.dev
oc new-project gogs
oc new-app -f http://bit.ly/openshift-gogs-persistent-template --param=HOSTNAME=gogs.apps.0xshift.dev
```

#### local persistent

```bash
oc new-app -f admin/configs/gogs-persistent-template.yaml \
  --param=HOSTNAME=gogs-gogs.techlab-apps.puzzle.ch \
  --param=GOGS_VERSION=0.11.34 \
  --param=GOGS_VOLUME_CAPACITY=256Mi \
  --param=DB_VOLUME_CAPACITY=256Mi \
  -l app=gogs
```

#### local ephemeral

```bash
oc new-app -f admin/configs/gogs-template.yaml \
  --param=HOSTNAME=gogs-gogs.techlab-apps.puzzle.ch \
  --param=GOGS_VERSION=0.11.34 \
  -l app=gogs
```

### route

```bash
oc delete route gogs
oc create route edge gogs-edge --service=gogs --hostname='gogs-gogs.techlab-apps.puzzle.ch'
```

### git init

1. Signup as openshift
1. Create repos by hand

Import:

* git@ssh.gitlab.puzzle.ch:craaflaub/techlab.git
* https://github.com/duritong/ruby-ex
* https://github.com/duritong/docker-build-httpd
* Add APPUiO php example: https://github.com/appuio/example-php-sti-helloworld.git


```bash
git remote add gogs https://gogs-gogs.techlab-apps.puzzle.ch/openshift/techlab.git
git push -u gogs master
```

### user groups and access

#### grant access for all users

    for i in {1..30}; do oc policy add-role-to-user view user$i; done

TODO: do it by group

    oc adm policy add-role-to-group view techlab -n openshift-web-console

### Create techlab user group

    oc adm groups new techlab
    for i in {1..30}; do oc adm groups add-users techlab user$i; done

### Grant user view access to actual project

    oc project
    for i in {1..30}; do oc policy add-role-to-user view user$i; done

## requirements for the labs

### lab 01

* techlab user group
* add techlab user group view rights to the project openshift-web-console

### lab 02

* repo: this techlab repo
* repo: <https://github.com/appuio/example-php-sti-helloworld>
* war file: <https://github.com/appuio/hello-world-war/blob/master/repo/ch/appuio/hello-world-war/1.0.0/hello-world-war-1.0.0.war?raw=true>
* OpenShift ImageStream: openshift/php:7.1
* Docker Hub Image: "openshift/wildfly-160-centos7"
* Docker Hub Image: "centos/httpd-24-centos7"

### lab 03

* repo: this techlab repo
* Docker Hub Image: "appuio/example-spring-boot"
* OpenShift Template: openshift/mysql-persistent
* PV mit 256Mi
* sql Dump file: <https://raw.githubusercontent.com/appuio/techlab/lab-3.3/labs/data/08_dump/dump.sql>

### lab 04

* OpenShift Project from lab 03: userXY-develop
* repo: https://github.com/chrira/ruby-ex.git#load
* OpenShift ImageStream: : "openshift/ruby:2.5"
* OpenShift metrics server: <https://docs.openshift.com/container-platform/3.11/dev_guide/pod_autoscaling.html>
  * Test: `oc get project | grep openshift-metrics-server`

### lab 05

* repo: <https://github.com/jbossdemocentral/coolstore-microservice/>
  * <https://github.com/jbossdemocentral/coolstore-microservice/#troubleshooting>

### lab 07

* repo: this techlab repo
* Docker Hub Image: "fabric8/s2i-java"
* Docker Hub Image: "openshift/oauth-proxy:v1.0.0"
* Docker Hub Image: "openshift/prometheus:v2.0.0"
* Docker Hub Image: "openshift/oauth-proxy:v1.0.0"
* Docker Hub Image: "openshift/prometheus-alert-buffer:v0.0.2"
* Docker Hub Image: "openshift/prometheus-alertmanager:v0.9.1"

### lab 08

* repo: this techlab repo
* repo: <https://github.com/appuio/example-php-sti-helloworld>
* OpenShift ImageStream: "openshift/php:7.0"
* Docker Hub Image: "openshift/jenkins-slave-maven-centos7"
* Red Hat Docker Image: "registry.access.redhat.com/openshift3/jenkins-slave-maven-rhel7:v3.11"
