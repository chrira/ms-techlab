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
