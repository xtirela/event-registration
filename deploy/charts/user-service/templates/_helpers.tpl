{{- define "user-service.fullname" -}}
{{- default .Chart.Name .Values.fullnameOverride -}}
{{- end }}