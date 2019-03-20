# Setup

Notes for setup

## Users

On bastion host:

    ansible masters -m shell -a 'for i in `seq 1 25`; do htpasswd -b /etc/origin/master/htpasswd user$i 0p3nSh1ftuser$i; done'
    ansible masters[0] -m shell -a 'oc adm groups new techlab; for i in `seq 1 25`; do oc adm groups add-users techlab user$i; done'

## Gogs

    oc login -u ocpadmin https://api.0xshift.dev
    oc new-project gogs
    oc new-app -f http://bit.ly/openshift-gogs-persistent-template --param=HOSTNAME=gogs.apps.0xshift.dev

Signup as ocpadmin

Import:

* https://github.com/duritong/ruby-ex
* https://github.com/duritong/docker-build-httpd
