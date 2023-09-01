# Description

Simple program to decode Helm release + revision secret. Sample output

Namespace: default release sample revision: 3
```
------------------------------------------------------------------
Namespace: default release sample revision: 3
--------------------------------
Chart: sample
Status: deployed
--------------------------------
Values:
{
"affinity" : { },
"autoscaling" : {
"enabled" : false,
"maxReplicas" : 100,
"minReplicas" : 1,
"targetCPUUtilizationPercentage" : 80
},
"fullnameOverride" : "",
"image" : {
"pullPolicy" : "IfNotPresent",
"repository" : "nginx",
"tag" : ""
},
"imagePullSecrets" : [ ],
"ingress" : {
"annotations" : { },
"className" : "",
"enabled" : false,
"hosts" : [ {
"host" : "chart-example.local",
"paths" : [ {
"path" : "/",
"pathType" : "ImplementationSpecific"
} ]
} ],
"tls" : [ ]
},
"nameOverride" : "",
"nodeSelector" : { },
"podAnnotations" : { },
"podSecurityContext" : { },
"replicaCount" : 1,
"resources" : { },
"securityContext" : { },
"service" : {
"port" : 80,
"type" : "ClusterIP"
},
"serviceAccount" : {
"annotations" : {
"kaka" : "kua",
"mama" : "mia"
},
"create" : true,
"name" : ""
},
"tolerations" : [ ]
}
--------------------------------
Templates:
Template: templates/NOTES.txt
Chart:
------
{{ .Chart | toYaml }}

Release:
--------
{{ .Release | toYaml }}


Capabilities:
-------------
{{ .Capabilities | toYaml }}


----
Template: templates/_helpers.tpl
{{/*
Expand the name of the chart.
*/}}
{{- define "sample.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "sample.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "sample.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "sample.labels" -}}
helm.sh/chart: {{ include "sample.chart" . }}
{{ include "sample.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "sample.selectorLabels" -}}
app.kubernetes.io/name: {{ include "sample.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "sample.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "sample.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

----
Template: templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
{{- if not .Values.autoscaling.enabled }}
replicas: {{ .Values.replicaCount }}
{{- end }}
selector:
matchLabels:
{{- include "sample.selectorLabels" . | nindent 6 }}
template:
metadata:
{{- with .Values.podAnnotations }}
annotations:
{{- toYaml . | nindent 8 }}
{{- end }}
labels:
{{- include "sample.selectorLabels" . | nindent 8 }}
spec:
{{- with .Values.imagePullSecrets }}
imagePullSecrets:
{{- toYaml . | nindent 8 }}
{{- end }}
serviceAccountName: {{ include "sample.serviceAccountName" . }}
securityContext:
{{- toYaml .Values.podSecurityContext | nindent 8 }}
containers:
- name: {{ .Chart.Name }}
securityContext:
{{- toYaml .Values.securityContext | nindent 12 }}
image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
imagePullPolicy: {{ .Values.image.pullPolicy }}
ports:
- name: http
containerPort: 80
protocol: TCP
livenessProbe:
httpGet:
path: /
port: http
readinessProbe:
httpGet:
path: /
port: http
resources:
{{- toYaml .Values.resources | nindent 12 }}
{{- with .Values.nodeSelector }}
nodeSelector:
{{- toYaml . | nindent 8 }}
{{- end }}
{{- with .Values.affinity }}
affinity:
{{- toYaml . | nindent 8 }}
{{- end }}
{{- with .Values.tolerations }}
tolerations:
{{- toYaml . | nindent 8 }}
{{- end }}

----
Template: templates/hpa.yaml
{{- if .Values.autoscaling.enabled }}
apiVersion: autoscaling/v2beta1
kind: HorizontalPodAutoscaler
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
scaleTargetRef:
apiVersion: apps/v1
kind: Deployment
name: {{ include "sample.fullname" . }}
minReplicas: {{ .Values.autoscaling.minReplicas }}
maxReplicas: {{ .Values.autoscaling.maxReplicas }}
metrics:
{{- if .Values.autoscaling.targetCPUUtilizationPercentage }}
- type: Resource
resource:
name: cpu
targetAverageUtilization: {{ .Values.autoscaling.targetCPUUtilizationPercentage }}
{{- end }}
{{- if .Values.autoscaling.targetMemoryUtilizationPercentage }}
- type: Resource
resource:
name: memory
targetAverageUtilization: {{ .Values.autoscaling.targetMemoryUtilizationPercentage }}
{{- end }}
{{- end }}

----
Template: templates/ingress.yaml
{{- if .Values.ingress.enabled -}}
{{- $fullName := include "sample.fullname" . -}}
{{- $svcPort := .Values.service.port -}}
{{- if and .Values.ingress.className (not (semverCompare ">=1.18-0" .Capabilities.KubeVersion.GitVersion)) }}
{{- if not (hasKey .Values.ingress.annotations "kubernetes.io/ingress.class") }}
{{- $_ := set .Values.ingress.annotations "kubernetes.io/ingress.class" .Values.ingress.className}}
{{- end }}
{{- end }}
{{- if semverCompare ">=1.19-0" .Capabilities.KubeVersion.GitVersion -}}
apiVersion: networking.k8s.io/v1
{{- else if semverCompare ">=1.14-0" .Capabilities.KubeVersion.GitVersion -}}
apiVersion: networking.k8s.io/v1beta1
{{- else -}}
apiVersion: extensions/v1beta1
{{- end }}
kind: Ingress
metadata:
name: {{ $fullName }}
labels:
{{- include "sample.labels" . | nindent 4 }}
{{- with .Values.ingress.annotations }}
annotations:
{{- toYaml . | nindent 4 }}
{{- end }}
spec:
{{- if and .Values.ingress.className (semverCompare ">=1.18-0" .Capabilities.KubeVersion.GitVersion) }}
ingressClassName: {{ .Values.ingress.className }}
{{- end }}
{{- if .Values.ingress.tls }}
tls:
{{- range .Values.ingress.tls }}
- hosts:
{{- range .hosts }}
- {{ . | quote }}
{{- end }}
secretName: {{ .secretName }}
{{- end }}
{{- end }}
rules:
{{- range .Values.ingress.hosts }}
- host: {{ .host | quote }}
http:
paths:
{{- range .paths }}
- path: {{ .path }}
{{- if and .pathType (semverCompare ">=1.18-0" $.Capabilities.KubeVersion.GitVersion) }}
pathType: {{ .pathType }}
{{- end }}
backend:
{{- if semverCompare ">=1.19-0" $.Capabilities.KubeVersion.GitVersion }}
service:
name: {{ $fullName }}
port:
number: {{ $svcPort }}
{{- else }}
serviceName: {{ $fullName }}
servicePort: {{ $svcPort }}
{{- end }}
{{- end }}
{{- end }}
{{- end }}

----
Template: templates/service.yaml
apiVersion: v1
kind: Service
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
type: {{ .Values.service.type }}
ports:
- port: {{ .Values.service.port }}
targetPort: http
protocol: TCP
name: http
selector:
{{- include "sample.selectorLabels" . | nindent 4 }}

----
Template: templates/serviceaccount.yaml
{{- if .Values.serviceAccount.create -}}
apiVersion: v1
kind: ServiceAccount
metadata:
name: {{ include "sample.serviceAccountName" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
{{- with .Values.serviceAccount.annotations }}
annotations:
{{- toYaml . | nindent 4 }}
{{- end }}
{{- end }}

----
Template: templates/tests/test-connection.yaml
apiVersion: v1
kind: Pod
metadata:
name: "{{ include "sample.fullname" . }}-test-connection"
labels:
{{- include "sample.labels" . | nindent 4 }}
annotations:
"helm.sh/hook": test
spec:
containers:
- name: wget
image: busybox
command: ['wget']
args: ['{{ include "sample.fullname" . }}:{{ .Values.service.port }}']
restartPolicy: Never

----
--------------------------------
Manifest:
---
# Source: sample/templates/serviceaccount.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
annotations:
kaka: kua
mama: mia
---
# Source: sample/templates/service.yaml
apiVersion: v1
kind: Service
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
spec:
type: ClusterIP
ports:
- port: 80
targetPort: http
protocol: TCP
name: http
selector:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
---
# Source: sample/templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
spec:
replicas: 1
selector:
matchLabels:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
template:
metadata:
labels:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
spec:
serviceAccountName: sample
securityContext:
{}
containers:
- name: sample
securityContext:
{}
image: "nginx:1.16.0"
imagePullPolicy: IfNotPresent
ports:
- name: http
containerPort: 80
protocol: TCP
livenessProbe:
httpGet:
path: /
port: http
readinessProbe:
httpGet:
path: /
port: http
resources:
{}

--------------------------------
Hooks:
Hook: sample/templates/tests/test-connection.yaml Events: ["test"]
apiVersion: v1
kind: Pod
metadata:
name: "sample-test-connection"
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
annotations:
"helm.sh/hook": test
spec:
containers:
- name: wget
image: busybox
command: ['wget']
args: ['sample:80']
restartPolicy: Never
----
--------------------------------
Notes:
Chart:
------
IsRoot: true
apiVersion: v2
appVersion: 1.16.0
description: A Helm chart for Kubernetes
name: sample
type: application
version: 0.1.0

Release:
--------
IsInstall: false
IsUpgrade: true
Name: sample
Namespace: default
Revision: 3
Service: Helm


Capabilities:
-------------
APIVersions:
- networking.k8s.io/v1
- apps/v1/ReplicaSet
- rbac.authorization.k8s.io/v1/ClusterRole
- autoscaling/v1
- apps/v1/StatefulSet
- certificates.k8s.io/v1/CertificateSigningRequest
- storage.k8s.io/v1/VolumeAttachment
- node.k8s.io/v1/RuntimeClass
- authentication.k8s.io/v1
- batch/v1
- rbac.authorization.k8s.io/v1
- v1/PodPortForwardOptions
- v1/PodProxyOptions
- flowcontrol.apiserver.k8s.io/v1beta2/PriorityLevelConfiguration
- v1
- flowcontrol.apiserver.k8s.io/v1beta2
- v1/Endpoints
- v1/TokenRequest
- batch/v1/Job
- networking.k8s.io/v1/IngressClass
- storage.k8s.io/v1beta1/CSIStorageCapacity
- admissionregistration.k8s.io/v1/ValidatingWebhookConfiguration
- apiextensions.k8s.io/v1/CustomResourceDefinition
- scheduling.k8s.io/v1/PriorityClass
- discovery.k8s.io/v1/EndpointSlice
- apiregistration.k8s.io/v1
- policy/v1
- node.k8s.io/v1
- v1/LimitRange
- v1/ServiceProxyOptions
- v1/PodAttachOptions
- authorization.k8s.io/v1/LocalSubjectAccessReview
- storage.k8s.io/v1/CSIStorageCapacity
- storage.k8s.io/v1
- scheduling.k8s.io/v1
- v1/ComponentStatus
- v1/ConfigMap
- v1/Event
- v1/PersistentVolume
- apps/v1/DaemonSet
- events.k8s.io/v1/Event
- authorization.k8s.io/v1/SelfSubjectRulesReview
- networking.k8s.io/v1/Ingress
- v1/PersistentVolumeClaim
- v1/ReplicationController
- storage.k8s.io/v1/CSINode
- flowcontrol.apiserver.k8s.io/v1beta1/FlowSchema
- v1/Scale
- v1/ServiceAccount
- apps/v1/Scale
- storage.k8s.io/v1/CSIDriver
- authorization.k8s.io/v1
- coordination.k8s.io/v1
- v1/PodExecOptions
- v1/Secret
- networking.k8s.io/v1/NetworkPolicy
- admissionregistration.k8s.io/v1/MutatingWebhookConfiguration
- coordination.k8s.io/v1/Lease
- storage.k8s.io/v1beta1
- discovery.k8s.io/v1
- v1/ResourceQuota
- policy/v1/PodDisruptionBudget
- flowcontrol.apiserver.k8s.io/v1beta2/FlowSchema
- v1/NodeProxyOptions
- authorization.k8s.io/v1/SelfSubjectAccessReview
- flowcontrol.apiserver.k8s.io/v1beta1/PriorityLevelConfiguration
- apps/v1
- events.k8s.io/v1
- apps/v1/ControllerRevision
- rbac.authorization.k8s.io/v1/ClusterRoleBinding
- autoscaling/v2
- apiextensions.k8s.io/v1
- autoscaling/v2/HorizontalPodAutoscaler
- autoscaling/v1/HorizontalPodAutoscaler
- rbac.authorization.k8s.io/v1/RoleBinding
- certificates.k8s.io/v1
- flowcontrol.apiserver.k8s.io/v1beta1
- v1/Binding
- v1/Pod
- v1/PodTemplate
- apps/v1/Deployment
- authorization.k8s.io/v1/SubjectAccessReview
- autoscaling/v2beta2/HorizontalPodAutoscaler
- rbac.authorization.k8s.io/v1/Role
- autoscaling/v2beta2
- admissionregistration.k8s.io/v1
- v1/Namespace
- v1/Node
- v1/Eviction
- v1/Service
- apiregistration.k8s.io/v1/APIService
- authentication.k8s.io/v1/TokenReview
- batch/v1/CronJob
- storage.k8s.io/v1/StorageClass
HelmVersion: {}
KubeVersion:
Major: "1"
Minor: "25"
Version: v1.25.4


------------------------------------------------------------------
------------------------------------------------------------------
Namespace: default release sample revision: 2
--------------------------------
Chart: sample
Status: superseded
--------------------------------
Values:
{
"affinity" : { },
"autoscaling" : {
"enabled" : false,
"maxReplicas" : 100,
"minReplicas" : 1,
"targetCPUUtilizationPercentage" : 80
},
"fullnameOverride" : "",
"image" : {
"pullPolicy" : "IfNotPresent",
"repository" : "nginx",
"tag" : ""
},
"imagePullSecrets" : [ ],
"ingress" : {
"annotations" : { },
"className" : "",
"enabled" : false,
"hosts" : [ {
"host" : "chart-example.local",
"paths" : [ {
"path" : "/",
"pathType" : "ImplementationSpecific"
} ]
} ],
"tls" : [ ]
},
"nameOverride" : "",
"nodeSelector" : { },
"podAnnotations" : { },
"podSecurityContext" : { },
"replicaCount" : 2,
"resources" : { },
"securityContext" : { },
"service" : {
"port" : 80,
"type" : "ClusterIP"
},
"serviceAccount" : {
"annotations" : {
"mama" : "mia"
},
"create" : true,
"name" : ""
},
"tolerations" : [ ]
}
--------------------------------
Templates:
Template: templates/NOTES.txt
Chart:
------
{{ .Chart | toYaml }}

Release:
--------
{{ .Release | toYaml }}


Capabilities:
-------------
{{ .Capabilities | toYaml }}


----
Template: templates/_helpers.tpl
{{/*
Expand the name of the chart.
*/}}
{{- define "sample.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "sample.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "sample.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "sample.labels" -}}
helm.sh/chart: {{ include "sample.chart" . }}
{{ include "sample.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "sample.selectorLabels" -}}
app.kubernetes.io/name: {{ include "sample.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "sample.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "sample.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

----
Template: templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
{{- if not .Values.autoscaling.enabled }}
replicas: {{ .Values.replicaCount }}
{{- end }}
selector:
matchLabels:
{{- include "sample.selectorLabels" . | nindent 6 }}
template:
metadata:
{{- with .Values.podAnnotations }}
annotations:
{{- toYaml . | nindent 8 }}
{{- end }}
labels:
{{- include "sample.selectorLabels" . | nindent 8 }}
spec:
{{- with .Values.imagePullSecrets }}
imagePullSecrets:
{{- toYaml . | nindent 8 }}
{{- end }}
serviceAccountName: {{ include "sample.serviceAccountName" . }}
securityContext:
{{- toYaml .Values.podSecurityContext | nindent 8 }}
containers:
- name: {{ .Chart.Name }}
securityContext:
{{- toYaml .Values.securityContext | nindent 12 }}
image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
imagePullPolicy: {{ .Values.image.pullPolicy }}
ports:
- name: http
containerPort: 80
protocol: TCP
livenessProbe:
httpGet:
path: /
port: http
readinessProbe:
httpGet:
path: /
port: http
resources:
{{- toYaml .Values.resources | nindent 12 }}
{{- with .Values.nodeSelector }}
nodeSelector:
{{- toYaml . | nindent 8 }}
{{- end }}
{{- with .Values.affinity }}
affinity:
{{- toYaml . | nindent 8 }}
{{- end }}
{{- with .Values.tolerations }}
tolerations:
{{- toYaml . | nindent 8 }}
{{- end }}

----
Template: templates/hpa.yaml
{{- if .Values.autoscaling.enabled }}
apiVersion: autoscaling/v2beta1
kind: HorizontalPodAutoscaler
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
scaleTargetRef:
apiVersion: apps/v1
kind: Deployment
name: {{ include "sample.fullname" . }}
minReplicas: {{ .Values.autoscaling.minReplicas }}
maxReplicas: {{ .Values.autoscaling.maxReplicas }}
metrics:
{{- if .Values.autoscaling.targetCPUUtilizationPercentage }}
- type: Resource
resource:
name: cpu
targetAverageUtilization: {{ .Values.autoscaling.targetCPUUtilizationPercentage }}
{{- end }}
{{- if .Values.autoscaling.targetMemoryUtilizationPercentage }}
- type: Resource
resource:
name: memory
targetAverageUtilization: {{ .Values.autoscaling.targetMemoryUtilizationPercentage }}
{{- end }}
{{- end }}

----
Template: templates/ingress.yaml
{{- if .Values.ingress.enabled -}}
{{- $fullName := include "sample.fullname" . -}}
{{- $svcPort := .Values.service.port -}}
{{- if and .Values.ingress.className (not (semverCompare ">=1.18-0" .Capabilities.KubeVersion.GitVersion)) }}
{{- if not (hasKey .Values.ingress.annotations "kubernetes.io/ingress.class") }}
{{- $_ := set .Values.ingress.annotations "kubernetes.io/ingress.class" .Values.ingress.className}}
{{- end }}
{{- end }}
{{- if semverCompare ">=1.19-0" .Capabilities.KubeVersion.GitVersion -}}
apiVersion: networking.k8s.io/v1
{{- else if semverCompare ">=1.14-0" .Capabilities.KubeVersion.GitVersion -}}
apiVersion: networking.k8s.io/v1beta1
{{- else -}}
apiVersion: extensions/v1beta1
{{- end }}
kind: Ingress
metadata:
name: {{ $fullName }}
labels:
{{- include "sample.labels" . | nindent 4 }}
{{- with .Values.ingress.annotations }}
annotations:
{{- toYaml . | nindent 4 }}
{{- end }}
spec:
{{- if and .Values.ingress.className (semverCompare ">=1.18-0" .Capabilities.KubeVersion.GitVersion) }}
ingressClassName: {{ .Values.ingress.className }}
{{- end }}
{{- if .Values.ingress.tls }}
tls:
{{- range .Values.ingress.tls }}
- hosts:
{{- range .hosts }}
- {{ . | quote }}
{{- end }}
secretName: {{ .secretName }}
{{- end }}
{{- end }}
rules:
{{- range .Values.ingress.hosts }}
- host: {{ .host | quote }}
http:
paths:
{{- range .paths }}
- path: {{ .path }}
{{- if and .pathType (semverCompare ">=1.18-0" $.Capabilities.KubeVersion.GitVersion) }}
pathType: {{ .pathType }}
{{- end }}
backend:
{{- if semverCompare ">=1.19-0" $.Capabilities.KubeVersion.GitVersion }}
service:
name: {{ $fullName }}
port:
number: {{ $svcPort }}
{{- else }}
serviceName: {{ $fullName }}
servicePort: {{ $svcPort }}
{{- end }}
{{- end }}
{{- end }}
{{- end }}

----
Template: templates/service.yaml
apiVersion: v1
kind: Service
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
type: {{ .Values.service.type }}
ports:
- port: {{ .Values.service.port }}
targetPort: http
protocol: TCP
name: http
selector:
{{- include "sample.selectorLabels" . | nindent 4 }}

----
Template: templates/serviceaccount.yaml
{{- if .Values.serviceAccount.create -}}
apiVersion: v1
kind: ServiceAccount
metadata:
name: {{ include "sample.serviceAccountName" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
{{- with .Values.serviceAccount.annotations }}
annotations:
{{- toYaml . | nindent 4 }}
{{- end }}
{{- end }}

----
Template: templates/tests/test-connection.yaml
apiVersion: v1
kind: Pod
metadata:
name: "{{ include "sample.fullname" . }}-test-connection"
labels:
{{- include "sample.labels" . | nindent 4 }}
annotations:
"helm.sh/hook": test
spec:
containers:
- name: wget
image: busybox
command: ['wget']
args: ['{{ include "sample.fullname" . }}:{{ .Values.service.port }}']
restartPolicy: Never

----
--------------------------------
Manifest:
---
# Source: sample/templates/serviceaccount.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
annotations:
mama: mia
---
# Source: sample/templates/service.yaml
apiVersion: v1
kind: Service
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
spec:
type: ClusterIP
ports:
- port: 80
targetPort: http
protocol: TCP
name: http
selector:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
---
# Source: sample/templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
spec:
replicas: 2
selector:
matchLabels:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
template:
metadata:
labels:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
spec:
serviceAccountName: sample
securityContext:
{}
containers:
- name: sample
securityContext:
{}
image: "nginx:1.16.0"
imagePullPolicy: IfNotPresent
ports:
- name: http
containerPort: 80
protocol: TCP
livenessProbe:
httpGet:
path: /
port: http
readinessProbe:
httpGet:
path: /
port: http
resources:
{}

--------------------------------
Hooks:
Hook: sample/templates/tests/test-connection.yaml Events: ["test"]
apiVersion: v1
kind: Pod
metadata:
name: "sample-test-connection"
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
annotations:
"helm.sh/hook": test
spec:
containers:
- name: wget
image: busybox
command: ['wget']
args: ['sample:80']
restartPolicy: Never
----
--------------------------------
Notes:
Chart:
------
IsRoot: true
apiVersion: v2
appVersion: 1.16.0
description: A Helm chart for Kubernetes
name: sample
type: application
version: 0.1.0

Release:
--------
IsInstall: false
IsUpgrade: true
Name: sample
Namespace: default
Revision: 2
Service: Helm


Capabilities:
-------------
APIVersions:
- rbac.authorization.k8s.io/v1/RoleBinding
- apiextensions.k8s.io/v1/CustomResourceDefinition
- v1
- v1/PodPortForwardOptions
- apiregistration.k8s.io/v1
- authentication.k8s.io/v1
- discovery.k8s.io/v1
- batch/v1/CronJob
- authorization.k8s.io/v1
- autoscaling/v2
- batch/v1
- certificates.k8s.io/v1
- networking.k8s.io/v1
- v1/ComponentStatus
- v1/PodAttachOptions
- v1/PodTemplate
- apps/v1/DaemonSet
- v1/Node
- v1/PersistentVolumeClaim
- authorization.k8s.io/v1/SubjectAccessReview
- storage.k8s.io/v1beta1/CSIStorageCapacity
- flowcontrol.apiserver.k8s.io/v1beta2/PriorityLevelConfiguration
- rbac.authorization.k8s.io/v1
- v1/Scale
- policy/v1/PodDisruptionBudget
- events.k8s.io/v1
- storage.k8s.io/v1beta1
- flowcontrol.apiserver.k8s.io/v1beta1
- v1/Secret
- apps/v1/Scale
- storage.k8s.io/v1/CSINode
- admissionregistration.k8s.io/v1/MutatingWebhookConfiguration
- flowcontrol.apiserver.k8s.io/v1beta1/FlowSchema
- autoscaling/v2beta2
- policy/v1
- storage.k8s.io/v1
- v1/PodExecOptions
- v1/ResourceQuota
- v1/TokenRequest
- authentication.k8s.io/v1/TokenReview
- authorization.k8s.io/v1/SelfSubjectRulesReview
- rbac.authorization.k8s.io/v1/Role
- admissionregistration.k8s.io/v1/ValidatingWebhookConfiguration
- autoscaling/v1
- v1/LimitRange
- v1/ServiceAccount
- apiregistration.k8s.io/v1/APIService
- apps/v1/ControllerRevision
- authorization.k8s.io/v1/LocalSubjectAccessReview
- networking.k8s.io/v1/IngressClass
- storage.k8s.io/v1/CSIDriver
- node.k8s.io/v1
- flowcontrol.apiserver.k8s.io/v1beta2
- v1/ReplicationController
- networking.k8s.io/v1/NetworkPolicy
- rbac.authorization.k8s.io/v1/ClusterRoleBinding
- rbac.authorization.k8s.io/v1/ClusterRole
- scheduling.k8s.io/v1/PriorityClass
- v1/ConfigMap
- v1/Pod
- events.k8s.io/v1/Event
- storage.k8s.io/v1/VolumeAttachment
- apps/v1
- scheduling.k8s.io/v1
- coordination.k8s.io/v1
- v1/Eviction
- v1/ServiceProxyOptions
- storage.k8s.io/v1/CSIStorageCapacity
- flowcontrol.apiserver.k8s.io/v1beta1/PriorityLevelConfiguration
- v1/Binding
- autoscaling/v1/HorizontalPodAutoscaler
- certificates.k8s.io/v1/CertificateSigningRequest
- networking.k8s.io/v1/Ingress
- v1/Endpoints
- v1/NodeProxyOptions
- v1/Service
- apps/v1/Deployment
- storage.k8s.io/v1/StorageClass
- admissionregistration.k8s.io/v1
- apiextensions.k8s.io/v1
- v1/PersistentVolume
- apps/v1/ReplicaSet
- apps/v1/StatefulSet
- batch/v1/Job
- coordination.k8s.io/v1/Lease
- discovery.k8s.io/v1/EndpointSlice
- flowcontrol.apiserver.k8s.io/v1beta2/FlowSchema
- v1/Event
- v1/Namespace
- v1/PodProxyOptions
- authorization.k8s.io/v1/SelfSubjectAccessReview
- autoscaling/v2/HorizontalPodAutoscaler
- autoscaling/v2beta2/HorizontalPodAutoscaler
- node.k8s.io/v1/RuntimeClass
HelmVersion: {}
KubeVersion:
Major: "1"
Minor: "25"
Version: v1.25.4


------------------------------------------------------------------
------------------------------------------------------------------
Namespace: default release sample revision: 1
--------------------------------
Chart: sample
Status: superseded
--------------------------------
Values:
{
"affinity" : { },
"autoscaling" : {
"enabled" : false,
"maxReplicas" : 100,
"minReplicas" : 1,
"targetCPUUtilizationPercentage" : 80
},
"fullnameOverride" : "",
"image" : {
"pullPolicy" : "IfNotPresent",
"repository" : "nginx",
"tag" : ""
},
"imagePullSecrets" : [ ],
"ingress" : {
"annotations" : { },
"className" : "",
"enabled" : false,
"hosts" : [ {
"host" : "chart-example.local",
"paths" : [ {
"path" : "/",
"pathType" : "ImplementationSpecific"
} ]
} ],
"tls" : [ ]
},
"nameOverride" : "",
"nodeSelector" : { },
"podAnnotations" : { },
"podSecurityContext" : { },
"replicaCount" : 1,
"resources" : { },
"securityContext" : { },
"service" : {
"port" : 80,
"type" : "ClusterIP"
},
"serviceAccount" : {
"annotations" : { },
"create" : true,
"name" : ""
},
"tolerations" : [ ]
}
--------------------------------
Templates:
Template: templates/NOTES.txt
Chart:
------
{{ .Chart | toYaml }}

Release:
--------
{{ .Release | toYaml }}


Capabilities:
-------------
{{ .Capabilities | toYaml }}


----
Template: templates/_helpers.tpl
{{/*
Expand the name of the chart.
*/}}
{{- define "sample.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "sample.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "sample.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "sample.labels" -}}
helm.sh/chart: {{ include "sample.chart" . }}
{{ include "sample.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "sample.selectorLabels" -}}
app.kubernetes.io/name: {{ include "sample.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "sample.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "sample.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

----
Template: templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
{{- if not .Values.autoscaling.enabled }}
replicas: {{ .Values.replicaCount }}
{{- end }}
selector:
matchLabels:
{{- include "sample.selectorLabels" . | nindent 6 }}
template:
metadata:
{{- with .Values.podAnnotations }}
annotations:
{{- toYaml . | nindent 8 }}
{{- end }}
labels:
{{- include "sample.selectorLabels" . | nindent 8 }}
spec:
{{- with .Values.imagePullSecrets }}
imagePullSecrets:
{{- toYaml . | nindent 8 }}
{{- end }}
serviceAccountName: {{ include "sample.serviceAccountName" . }}
securityContext:
{{- toYaml .Values.podSecurityContext | nindent 8 }}
containers:
- name: {{ .Chart.Name }}
securityContext:
{{- toYaml .Values.securityContext | nindent 12 }}
image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
imagePullPolicy: {{ .Values.image.pullPolicy }}
ports:
- name: http
containerPort: 80
protocol: TCP
livenessProbe:
httpGet:
path: /
port: http
readinessProbe:
httpGet:
path: /
port: http
resources:
{{- toYaml .Values.resources | nindent 12 }}
{{- with .Values.nodeSelector }}
nodeSelector:
{{- toYaml . | nindent 8 }}
{{- end }}
{{- with .Values.affinity }}
affinity:
{{- toYaml . | nindent 8 }}
{{- end }}
{{- with .Values.tolerations }}
tolerations:
{{- toYaml . | nindent 8 }}
{{- end }}

----
Template: templates/hpa.yaml
{{- if .Values.autoscaling.enabled }}
apiVersion: autoscaling/v2beta1
kind: HorizontalPodAutoscaler
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
scaleTargetRef:
apiVersion: apps/v1
kind: Deployment
name: {{ include "sample.fullname" . }}
minReplicas: {{ .Values.autoscaling.minReplicas }}
maxReplicas: {{ .Values.autoscaling.maxReplicas }}
metrics:
{{- if .Values.autoscaling.targetCPUUtilizationPercentage }}
- type: Resource
resource:
name: cpu
targetAverageUtilization: {{ .Values.autoscaling.targetCPUUtilizationPercentage }}
{{- end }}
{{- if .Values.autoscaling.targetMemoryUtilizationPercentage }}
- type: Resource
resource:
name: memory
targetAverageUtilization: {{ .Values.autoscaling.targetMemoryUtilizationPercentage }}
{{- end }}
{{- end }}

----
Template: templates/ingress.yaml
{{- if .Values.ingress.enabled -}}
{{- $fullName := include "sample.fullname" . -}}
{{- $svcPort := .Values.service.port -}}
{{- if and .Values.ingress.className (not (semverCompare ">=1.18-0" .Capabilities.KubeVersion.GitVersion)) }}
{{- if not (hasKey .Values.ingress.annotations "kubernetes.io/ingress.class") }}
{{- $_ := set .Values.ingress.annotations "kubernetes.io/ingress.class" .Values.ingress.className}}
{{- end }}
{{- end }}
{{- if semverCompare ">=1.19-0" .Capabilities.KubeVersion.GitVersion -}}
apiVersion: networking.k8s.io/v1
{{- else if semverCompare ">=1.14-0" .Capabilities.KubeVersion.GitVersion -}}
apiVersion: networking.k8s.io/v1beta1
{{- else -}}
apiVersion: extensions/v1beta1
{{- end }}
kind: Ingress
metadata:
name: {{ $fullName }}
labels:
{{- include "sample.labels" . | nindent 4 }}
{{- with .Values.ingress.annotations }}
annotations:
{{- toYaml . | nindent 4 }}
{{- end }}
spec:
{{- if and .Values.ingress.className (semverCompare ">=1.18-0" .Capabilities.KubeVersion.GitVersion) }}
ingressClassName: {{ .Values.ingress.className }}
{{- end }}
{{- if .Values.ingress.tls }}
tls:
{{- range .Values.ingress.tls }}
- hosts:
{{- range .hosts }}
- {{ . | quote }}
{{- end }}
secretName: {{ .secretName }}
{{- end }}
{{- end }}
rules:
{{- range .Values.ingress.hosts }}
- host: {{ .host | quote }}
http:
paths:
{{- range .paths }}
- path: {{ .path }}
{{- if and .pathType (semverCompare ">=1.18-0" $.Capabilities.KubeVersion.GitVersion) }}
pathType: {{ .pathType }}
{{- end }}
backend:
{{- if semverCompare ">=1.19-0" $.Capabilities.KubeVersion.GitVersion }}
service:
name: {{ $fullName }}
port:
number: {{ $svcPort }}
{{- else }}
serviceName: {{ $fullName }}
servicePort: {{ $svcPort }}
{{- end }}
{{- end }}
{{- end }}
{{- end }}

----
Template: templates/service.yaml
apiVersion: v1
kind: Service
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
type: {{ .Values.service.type }}
ports:
- port: {{ .Values.service.port }}
targetPort: http
protocol: TCP
name: http
selector:
{{- include "sample.selectorLabels" . | nindent 4 }}

----
Template: templates/serviceaccount.yaml
{{- if .Values.serviceAccount.create -}}
apiVersion: v1
kind: ServiceAccount
metadata:
name: {{ include "sample.serviceAccountName" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
{{- with .Values.serviceAccount.annotations }}
annotations:
{{- toYaml . | nindent 4 }}
{{- end }}
{{- end }}

----
Template: templates/tests/test-connection.yaml
apiVersion: v1
kind: Pod
metadata:
name: "{{ include "sample.fullname" . }}-test-connection"
labels:
{{- include "sample.labels" . | nindent 4 }}
annotations:
"helm.sh/hook": test
spec:
containers:
- name: wget
image: busybox
command: ['wget']
args: ['{{ include "sample.fullname" . }}:{{ .Values.service.port }}']
restartPolicy: Never

----
--------------------------------
Manifest:
---
# Source: sample/templates/serviceaccount.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
---
# Source: sample/templates/service.yaml
apiVersion: v1
kind: Service
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
spec:
type: ClusterIP
ports:
- port: 80
targetPort: http
protocol: TCP
name: http
selector:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
---
# Source: sample/templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
spec:
replicas: 1
selector:
matchLabels:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
template:
metadata:
labels:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
spec:
serviceAccountName: sample
securityContext:
{}
containers:
- name: sample
securityContext:
{}
image: "nginx:1.16.0"
imagePullPolicy: IfNotPresent
ports:
- name: http
containerPort: 80
protocol: TCP
livenessProbe:
httpGet:
path: /
port: http
readinessProbe:
httpGet:
path: /
port: http
resources:
{}

--------------------------------
Hooks:
Hook: sample/templates/tests/test-connection.yaml Events: ["test"]
apiVersion: v1
kind: Pod
metadata:
name: "sample-test-connection"
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
annotations:
"helm.sh/hook": test
spec:
containers:
- name: wget
image: busybox
command: ['wget']
args: ['sample:80']
restartPolicy: Never
----
--------------------------------
Notes:
Chart:
------
IsRoot: true
apiVersion: v2
appVersion: 1.16.0
description: A Helm chart for Kubernetes
name: sample
type: application
version: 0.1.0

Release:
--------
IsInstall: true
IsUpgrade: false
Name: sample
Namespace: default
Revision: 1
Service: Helm


Capabilities:
-------------
APIVersions:
- node.k8s.io/v1
- v1/Event
- v1/LimitRange
- v1/TokenRequest
- apps/v1/Deployment
- certificates.k8s.io/v1/CertificateSigningRequest
- coordination.k8s.io/v1/Lease
- storage.k8s.io/v1
- flowcontrol.apiserver.k8s.io/v1beta2
- v1/Node
- v1/ServiceAccount
- apiregistration.k8s.io/v1/APIService
- autoscaling/v1/HorizontalPodAutoscaler
- networking.k8s.io/v1/NetworkPolicy
- flowcontrol.apiserver.k8s.io/v1beta2/FlowSchema
- certificates.k8s.io/v1
- v1/PersistentVolume
- networking.k8s.io/v1/Ingress
- admissionregistration.k8s.io/v1/ValidatingWebhookConfiguration
- flowcontrol.apiserver.k8s.io/v1beta1/FlowSchema
- policy/v1
- v1/PodExecOptions
- rbac.authorization.k8s.io/v1/ClusterRole
- v1/Pod
- v1/Scale
- apps/v1/StatefulSet
- events.k8s.io/v1/Event
- authorization.k8s.io/v1/SelfSubjectAccessReview
- autoscaling/v2/HorizontalPodAutoscaler
- storage.k8s.io/v1/StorageClass
- node.k8s.io/v1/RuntimeClass
- v1/PodAttachOptions
- admissionregistration.k8s.io/v1
- v1/PodPortForwardOptions
- v1/ServiceProxyOptions
- apps/v1/Scale
- apps/v1/ReplicaSet
- authorization.k8s.io/v1/SubjectAccessReview
- networking.k8s.io/v1
- v1/PersistentVolumeClaim
- v1/PodTemplate
- autoscaling/v2beta2/HorizontalPodAutoscaler
- storage.k8s.io/v1/CSINode
- discovery.k8s.io/v1
- storage.k8s.io/v1beta1
- apiextensions.k8s.io/v1
- scheduling.k8s.io/v1
- v1/Secret
- authorization.k8s.io/v1/LocalSubjectAccessReview
- batch/v1/CronJob
- storage.k8s.io/v1beta1/CSIStorageCapacity
- apiregistration.k8s.io/v1
- coordination.k8s.io/v1
- v1/Binding
- v1/ConfigMap
- v1/ReplicationController
- v1/ResourceQuota
- v1/Service
- storage.k8s.io/v1/CSIStorageCapacity
- autoscaling/v2
- flowcontrol.apiserver.k8s.io/v1beta1
- v1/ComponentStatus
- apps/v1/DaemonSet
- authentication.k8s.io/v1/TokenReview
- policy/v1/PodDisruptionBudget
- flowcontrol.apiserver.k8s.io/v1beta2/PriorityLevelConfiguration
- authentication.k8s.io/v1
- v1/Eviction
- networking.k8s.io/v1/IngressClass
- rbac.authorization.k8s.io/v1/Role
- storage.k8s.io/v1/VolumeAttachment
- apiextensions.k8s.io/v1/CustomResourceDefinition
- flowcontrol.apiserver.k8s.io/v1beta1/PriorityLevelConfiguration
- rbac.authorization.k8s.io/v1
- apps/v1/ControllerRevision
- apps/v1
- autoscaling/v2beta2
- v1/NodeProxyOptions
- discovery.k8s.io/v1/EndpointSlice
- v1
- authorization.k8s.io/v1/SelfSubjectRulesReview
- batch/v1/Job
- rbac.authorization.k8s.io/v1/ClusterRoleBinding
- autoscaling/v1
- batch/v1
- v1/Endpoints
- v1/Namespace
- v1/PodProxyOptions
- storage.k8s.io/v1/CSIDriver
- authorization.k8s.io/v1
- rbac.authorization.k8s.io/v1/RoleBinding
- admissionregistration.k8s.io/v1/MutatingWebhookConfiguration
- scheduling.k8s.io/v1/PriorityClass
- events.k8s.io/v1
HelmVersion: {}
KubeVersion:
Major: "1"
Minor: "25"
Version: v1.25.4


------------------------------------------------------------------
------------------------------------------------------------------
Namespace: sample release sample revision: 1
--------------------------------
Chart: sample
Status: deployed
--------------------------------
Values:
{
"affinity" : { },
"autoscaling" : {
"enabled" : false,
"maxReplicas" : 100,
"minReplicas" : 1,
"targetCPUUtilizationPercentage" : 80
},
"fullnameOverride" : "",
"image" : {
"pullPolicy" : "IfNotPresent",
"repository" : "nginx",
"tag" : ""
},
"imagePullSecrets" : [ ],
"ingress" : {
"annotations" : { },
"className" : "",
"enabled" : false,
"hosts" : [ {
"host" : "chart-example.local",
"paths" : [ {
"path" : "/",
"pathType" : "ImplementationSpecific"
} ]
} ],
"tls" : [ ]
},
"nameOverride" : "",
"nodeSelector" : { },
"podAnnotations" : { },
"podSecurityContext" : { },
"replicaCount" : 1,
"resources" : { },
"securityContext" : { },
"service" : {
"port" : 80,
"type" : "ClusterIP"
},
"serviceAccount" : {
"annotations" : {
"kaka" : "kua",
"mama" : "mia"
},
"create" : true,
"name" : ""
},
"tolerations" : [ ]
}
--------------------------------
Templates:
Template: templates/NOTES.txt
Chart:
------
{{ .Chart | toYaml }}

Release:
--------
{{ .Release | toYaml }}


Capabilities:
-------------
{{ .Capabilities | toYaml }}


----
Template: templates/_helpers.tpl
{{/*
Expand the name of the chart.
*/}}
{{- define "sample.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "sample.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "sample.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "sample.labels" -}}
helm.sh/chart: {{ include "sample.chart" . }}
{{ include "sample.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "sample.selectorLabels" -}}
app.kubernetes.io/name: {{ include "sample.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "sample.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "sample.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

----
Template: templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
{{- if not .Values.autoscaling.enabled }}
replicas: {{ .Values.replicaCount }}
{{- end }}
selector:
matchLabels:
{{- include "sample.selectorLabels" . | nindent 6 }}
template:
metadata:
{{- with .Values.podAnnotations }}
annotations:
{{- toYaml . | nindent 8 }}
{{- end }}
labels:
{{- include "sample.selectorLabels" . | nindent 8 }}
spec:
{{- with .Values.imagePullSecrets }}
imagePullSecrets:
{{- toYaml . | nindent 8 }}
{{- end }}
serviceAccountName: {{ include "sample.serviceAccountName" . }}
securityContext:
{{- toYaml .Values.podSecurityContext | nindent 8 }}
containers:
- name: {{ .Chart.Name }}
securityContext:
{{- toYaml .Values.securityContext | nindent 12 }}
image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
imagePullPolicy: {{ .Values.image.pullPolicy }}
ports:
- name: http
containerPort: 80
protocol: TCP
livenessProbe:
httpGet:
path: /
port: http
readinessProbe:
httpGet:
path: /
port: http
resources:
{{- toYaml .Values.resources | nindent 12 }}
{{- with .Values.nodeSelector }}
nodeSelector:
{{- toYaml . | nindent 8 }}
{{- end }}
{{- with .Values.affinity }}
affinity:
{{- toYaml . | nindent 8 }}
{{- end }}
{{- with .Values.tolerations }}
tolerations:
{{- toYaml . | nindent 8 }}
{{- end }}

----
Template: templates/hpa.yaml
{{- if .Values.autoscaling.enabled }}
apiVersion: autoscaling/v2beta1
kind: HorizontalPodAutoscaler
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
scaleTargetRef:
apiVersion: apps/v1
kind: Deployment
name: {{ include "sample.fullname" . }}
minReplicas: {{ .Values.autoscaling.minReplicas }}
maxReplicas: {{ .Values.autoscaling.maxReplicas }}
metrics:
{{- if .Values.autoscaling.targetCPUUtilizationPercentage }}
- type: Resource
resource:
name: cpu
targetAverageUtilization: {{ .Values.autoscaling.targetCPUUtilizationPercentage }}
{{- end }}
{{- if .Values.autoscaling.targetMemoryUtilizationPercentage }}
- type: Resource
resource:
name: memory
targetAverageUtilization: {{ .Values.autoscaling.targetMemoryUtilizationPercentage }}
{{- end }}
{{- end }}

----
Template: templates/ingress.yaml
{{- if .Values.ingress.enabled -}}
{{- $fullName := include "sample.fullname" . -}}
{{- $svcPort := .Values.service.port -}}
{{- if and .Values.ingress.className (not (semverCompare ">=1.18-0" .Capabilities.KubeVersion.GitVersion)) }}
{{- if not (hasKey .Values.ingress.annotations "kubernetes.io/ingress.class") }}
{{- $_ := set .Values.ingress.annotations "kubernetes.io/ingress.class" .Values.ingress.className}}
{{- end }}
{{- end }}
{{- if semverCompare ">=1.19-0" .Capabilities.KubeVersion.GitVersion -}}
apiVersion: networking.k8s.io/v1
{{- else if semverCompare ">=1.14-0" .Capabilities.KubeVersion.GitVersion -}}
apiVersion: networking.k8s.io/v1beta1
{{- else -}}
apiVersion: extensions/v1beta1
{{- end }}
kind: Ingress
metadata:
name: {{ $fullName }}
labels:
{{- include "sample.labels" . | nindent 4 }}
{{- with .Values.ingress.annotations }}
annotations:
{{- toYaml . | nindent 4 }}
{{- end }}
spec:
{{- if and .Values.ingress.className (semverCompare ">=1.18-0" .Capabilities.KubeVersion.GitVersion) }}
ingressClassName: {{ .Values.ingress.className }}
{{- end }}
{{- if .Values.ingress.tls }}
tls:
{{- range .Values.ingress.tls }}
- hosts:
{{- range .hosts }}
- {{ . | quote }}
{{- end }}
secretName: {{ .secretName }}
{{- end }}
{{- end }}
rules:
{{- range .Values.ingress.hosts }}
- host: {{ .host | quote }}
http:
paths:
{{- range .paths }}
- path: {{ .path }}
{{- if and .pathType (semverCompare ">=1.18-0" $.Capabilities.KubeVersion.GitVersion) }}
pathType: {{ .pathType }}
{{- end }}
backend:
{{- if semverCompare ">=1.19-0" $.Capabilities.KubeVersion.GitVersion }}
service:
name: {{ $fullName }}
port:
number: {{ $svcPort }}
{{- else }}
serviceName: {{ $fullName }}
servicePort: {{ $svcPort }}
{{- end }}
{{- end }}
{{- end }}
{{- end }}

----
Template: templates/service.yaml
apiVersion: v1
kind: Service
metadata:
name: {{ include "sample.fullname" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
spec:
type: {{ .Values.service.type }}
ports:
- port: {{ .Values.service.port }}
targetPort: http
protocol: TCP
name: http
selector:
{{- include "sample.selectorLabels" . | nindent 4 }}

----
Template: templates/serviceaccount.yaml
{{- if .Values.serviceAccount.create -}}
apiVersion: v1
kind: ServiceAccount
metadata:
name: {{ include "sample.serviceAccountName" . }}
labels:
{{- include "sample.labels" . | nindent 4 }}
{{- with .Values.serviceAccount.annotations }}
annotations:
{{- toYaml . | nindent 4 }}
{{- end }}
{{- end }}

----
Template: templates/tests/test-connection.yaml
apiVersion: v1
kind: Pod
metadata:
name: "{{ include "sample.fullname" . }}-test-connection"
labels:
{{- include "sample.labels" . | nindent 4 }}
annotations:
"helm.sh/hook": test
spec:
containers:
- name: wget
image: busybox
command: ['wget']
args: ['{{ include "sample.fullname" . }}:{{ .Values.service.port }}']
restartPolicy: Never

----
--------------------------------
Manifest:
---
# Source: sample/templates/serviceaccount.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
annotations:
kaka: kua
mama: mia
---
# Source: sample/templates/service.yaml
apiVersion: v1
kind: Service
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
spec:
type: ClusterIP
ports:
- port: 80
targetPort: http
protocol: TCP
name: http
selector:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
---
# Source: sample/templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
name: sample
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
spec:
replicas: 1
selector:
matchLabels:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
template:
metadata:
labels:
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
spec:
serviceAccountName: sample
securityContext:
{}
containers:
- name: sample
securityContext:
{}
image: "nginx:1.16.0"
imagePullPolicy: IfNotPresent
ports:
- name: http
containerPort: 80
protocol: TCP
livenessProbe:
httpGet:
path: /
port: http
readinessProbe:
httpGet:
path: /
port: http
resources:
{}

--------------------------------
Hooks:
Hook: sample/templates/tests/test-connection.yaml Events: ["test"]
apiVersion: v1
kind: Pod
metadata:
name: "sample-test-connection"
labels:
helm.sh/chart: sample-0.1.0
app.kubernetes.io/name: sample
app.kubernetes.io/instance: sample
app.kubernetes.io/version: "1.16.0"
app.kubernetes.io/managed-by: Helm
annotations:
"helm.sh/hook": test
spec:
containers:
- name: wget
image: busybox
command: ['wget']
args: ['sample:80']
restartPolicy: Never
----
--------------------------------
Notes:
Chart:
------
IsRoot: true
apiVersion: v2
appVersion: 1.16.0
description: A Helm chart for Kubernetes
name: sample
type: application
version: 0.1.0

Release:
--------
IsInstall: true
IsUpgrade: false
Name: sample
Namespace: sample
Revision: 1
Service: Helm


Capabilities:
-------------
APIVersions:
- admissionregistration.k8s.io/v1
- flowcontrol.apiserver.k8s.io/v1beta1
- v1/PersistentVolume
- v1/PodTemplate
- events.k8s.io/v1/Event
- authorization.k8s.io/v1/SelfSubjectAccessReview
- batch/v1/Job
- policy/v1
- rbac.authorization.k8s.io/v1/RoleBinding
- storage.k8s.io/v1/CSIStorageCapacity
- v1/ComponentStatus
- v1/Eviction
- rbac.authorization.k8s.io/v1/ClusterRole
- flowcontrol.apiserver.k8s.io/v1beta1/FlowSchema
- batch/v1
- v1/Pod
- v1/ServiceAccount
- networking.k8s.io/v1/Ingress
- storage.k8s.io/v1/CSIDriver
- storage.k8s.io/v1/StorageClass
- coordination.k8s.io/v1/Lease
- v1/Namespace
- v1/PodExecOptions
- apiregistration.k8s.io/v1/APIService
- storage.k8s.io/v1beta1/CSIStorageCapacity
- flowcontrol.apiserver.k8s.io/v1beta2/PriorityLevelConfiguration
- v1/Endpoints
- storage.k8s.io/v1beta1
- coordination.k8s.io/v1
- v1/ReplicationController
- apps/v1/ControllerRevision
- authorization.k8s.io/v1/LocalSubjectAccessReview
- autoscaling/v2/HorizontalPodAutoscaler
- certificates.k8s.io/v1
- rbac.authorization.k8s.io/v1
- v1/ResourceQuota
- v1/TokenRequest
- v1/ServiceProxyOptions
- events.k8s.io/v1
- discovery.k8s.io/v1
- v1/Scale
- batch/v1/CronJob
- networking.k8s.io/v1/NetworkPolicy
- node.k8s.io/v1/RuntimeClass
- autoscaling/v2beta2
- authentication.k8s.io/v1
- authentication.k8s.io/v1/TokenReview
- flowcontrol.apiserver.k8s.io/v1beta1/PriorityLevelConfiguration
- apiregistration.k8s.io/v1
- v1/Binding
- v1/PodProxyOptions
- apps/v1/ReplicaSet
- autoscaling/v2beta2/HorizontalPodAutoscaler
- v1
- v1/LimitRange
- v1/NodeProxyOptions
- v1/PodPortForwardOptions
- v1/Secret
- v1/Service
- v1/ConfigMap
- flowcontrol.apiserver.k8s.io/v1beta2
- storage.k8s.io/v1/CSINode
- flowcontrol.apiserver.k8s.io/v1beta2/FlowSchema
- apiextensions.k8s.io/v1
- apps/v1/StatefulSet
- authorization.k8s.io/v1/SubjectAccessReview
- apps/v1/DaemonSet
- certificates.k8s.io/v1/CertificateSigningRequest
- networking.k8s.io/v1/IngressClass
- rbac.authorization.k8s.io/v1/Role
- scheduling.k8s.io/v1/PriorityClass
- discovery.k8s.io/v1/EndpointSlice
- authorization.k8s.io/v1/SelfSubjectRulesReview
- autoscaling/v2
- autoscaling/v1
- networking.k8s.io/v1
- scheduling.k8s.io/v1
- node.k8s.io/v1
- v1/PersistentVolumeClaim
- v1/PodAttachOptions
- authorization.k8s.io/v1
- autoscaling/v1/HorizontalPodAutoscaler
- rbac.authorization.k8s.io/v1/ClusterRoleBinding
- storage.k8s.io/v1/VolumeAttachment
- admissionregistration.k8s.io/v1/MutatingWebhookConfiguration
- apiextensions.k8s.io/v1/CustomResourceDefinition
- apps/v1/Deployment
- storage.k8s.io/v1
- v1/Event
- v1/Node
- apps/v1/Scale
- policy/v1/PodDisruptionBudget
- admissionregistration.k8s.io/v1/ValidatingWebhookConfiguration
- apps/v1
HelmVersion: {}
KubeVersion:
Major: "1"
Minor: "25"
Version: v1.25.4


------------------------------------------------------------------
```

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.1.3/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.1.3/gradle-plugin/reference/html/#build-image)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

