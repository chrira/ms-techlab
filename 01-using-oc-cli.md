# Using oc cli

Log into the web console and get familiar with the interface.

Create a new project from the web console called:

    userXY-webui

## Log in on the cli

Copy the login command from the web console (did you find this option? -> in the menu on the right hand side).

    oc login https://api.0xshift.dev:443 --token=XYZ
    oc whoami

The token allows you to have a logged in session and can be used to do logins from the cli (on the API), without doing the authentication there.

Once you are logged in let's get familiar with the CLI and its commands.

## Getting help

oc cli features a help output, as well as more detailed help for each command:

    oc help
    oc projects -h
    oc projects

## Create a new project on the cli

Create a project called `userXY-cli`

You can get help by

    oc new-project -h

<details><summary>solution</summary>oc new-project userXY-cli</details><br/>

We are immediately switched to our project:

    oc project

We can inspect our project by either describing it or getting a yaml (or json) formatted output of our created project.

    oc describe project userXY-cli
    oc get project userXY-webui -o yaml
    oc get project userXY-webui -o json

## Adding users to a project

Openshift can have multiple users (also with different roles) on the same projects. For that we can add individual users on the project or we can also add a group of users to a project.

Users or groups can have different roles either within the whole cluster or locally within a project.

Find more about roles [here](https://docs.openshift.com/container-platform/3.11/architecture/additional_concepts/authorization.html#roles) and how to manage them [here](https://docs.openshift.com/container-platform/3.11/admin_guide/manage_rbac.html).

To see all the active roles in your current project you can type:

    oc describe rolebinding.rbac

For your webui project:

    oc describe rolebinding.rbac -n userXY-webui

We can mange roles by issuing oc adm policy commands:

    oc adm policy -h

For this lab there is a group called `techlab`, where all workshop users are being part of.

Let's add this group as an admin role to our current project, so we can co-develop things within these projects.

    oc adm policy add-role-to-group -h
    oc adm policy add-role-to-group admin techlab

Too much privileges? At least for our webui projects, so let's add folks there only as viewer:

    oc adm policy add-role-to-group view techlab -n userXY-webui

How many others did add us to their projects? Let's see by get the current list of projects:

    oc projects

## Inspecting and editing other resources

Everything within Openshift (Kubernetes) is represented as a resource, which we can view and depending on our privileges edit.

You can get all resources of your current project, by typing:

    oc get all

You can also get all resources of all namespaces (projects) you have access to:

    oc get all --all-namespaces

Take the gogs project that has some resources to inspect.
Click on command, if you did not find the solution how to add the namespace to the command.

<details><summary>command</summary>oc get all -n gogs</details><br/>

Found an interesting resource you want to know about it, you can describe/get each one of them:

<details><summary>general command</summary>oc describe resrourceXY resourceName -n gogs</details>
<details><summary>command to inspect a service</summary>oc describe service gogs -n gogs</details><br/>

You can also edit them:

    oc edit resrourceXY resourceName

For example let's edit our webui project.
<details><summary>command</summary>oc edit project userXY-webui</details><br/>

## Deleting resources

Not happy about how things went in your current projects and want to start over?

    oc delete project userXY-webui

This will delete all resources bundled by this project. Projects are really an easy way to try things out and once you are done easily clean it up.

## How are my resources doing?

You can always get an overview of your current resources by typing:

    oc status

This will become latery handy, once we start deploying more things.
