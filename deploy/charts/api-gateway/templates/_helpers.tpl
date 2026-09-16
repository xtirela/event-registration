{{- define "api-gateway.fullname" -}}
{{- default .Chart.Name .Values.fullnameOverride -}}
{{- end }}