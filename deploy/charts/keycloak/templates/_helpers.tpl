{{- define "keycloak.fullname" -}}
{{- default .Chart.Name .Values.fullnameOverride -}}
{{- end }}