# Compile le dossier en PDF : Markdown -> HTML (pandoc) -> PDF (WeasyPrint).
# Les schémas sont écrits en PlantUML (diagrammes/*.puml) et convertis en PNG avant la compilation ;
# diagrammes/style.puml contient le style commun à tous les schémas.
# Usage, depuis la racine du dépôt : make -C doc   (lancé automatiquement par GitHub Actions)
# Prérequis : pandoc, weasyprint, Java, Graphviz et plantuml.jar

PANDOC_OPTS = --standalone --css=style.css --pdf-engine=weasyprint -V document-css=false -M lang=fr
PLANTUML ?= java -jar plantuml.jar
DIAGRAMMES = $(patsubst %.puml,%.png,$(filter-out diagrammes/style.puml,$(wildcard diagrammes/*.puml)))

dossier.pdf: dossier.md style.css $(DIAGRAMMES) $(wildcard fonts/*)
	pandoc dossier.md -o $@ $(PANDOC_OPTS)

diagrammes/%.png: diagrammes/%.puml diagrammes/style.puml
	$(PLANTUML) -tpng -Sdpi=200 $<

.PHONY: clean
clean:
	rm -f dossier.pdf diagrammes/*.png
