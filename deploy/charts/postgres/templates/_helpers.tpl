{{- define "postgres.fullname" -}}
{{- .Values.fullnameOverride | default .Chart.Name -}}
{{- end }}