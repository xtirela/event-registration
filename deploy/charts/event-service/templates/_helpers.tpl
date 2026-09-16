{{- define "event-service.fullname" -}}
{{- default .Chart.Name .Values.fullnameOverride -}}
{{- end }}