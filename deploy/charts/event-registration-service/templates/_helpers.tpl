{{- define "event-registration-service.fullname" -}}
{{- default .Chart.Name .Values.fullnameOverride -}}
{{- end }}