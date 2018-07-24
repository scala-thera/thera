#!/usr/bin/env bash
pandoc \
  --toc \
  --webtex \
  --template=../site-src/templates/pandoc-post.html \
  --filter /pandoc-filters/pandocfilters/examples/graphviz.py \
  --filter /pandoc-filters/pandocfilters/examples/plantuml.py \
  --filter /pandoc-filters/include-code/include-code.py \
