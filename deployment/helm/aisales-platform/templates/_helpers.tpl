{{- define "aisales.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "aisales.fullname" -}}
{{- .Values.global.namespace -}}
{{- end -}}

{{- define "aisales.labels" -}}
app.kubernetes.io/part-of: ai-sales-platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{- define "aisales.image" -}}
{{- $registry := .root.Values.global.imageRegistry -}}
{{- $tag := .root.Values.global.imageTag -}}
{{- if and .color .root.Values.global.colorImageTags -}}
{{- $override := index .root.Values.global.colorImageTags .color -}}
{{- if $override -}}
{{- $tag = $override -}}
{{- end -}}
{{- end -}}
{{- printf "%s/%s:%s" $registry .image $tag -}}
{{- end -}}
