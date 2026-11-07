package handlers

import (
	"html/template"
)

var Templates *template.Template

func SetTemplates(t *template.Template) {
	Templates = t
}