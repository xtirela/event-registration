{{- define "participant-service.fullname" -}}
{{- default .Chart.Name .Values.fullnameOverride -}}
{{- end }}