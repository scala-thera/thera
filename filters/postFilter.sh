#!/usr/bin/env bash
pandoc \
  --toc \
  --webtex \
  --template=../site-src/templates/pandoc-post.html \
  --filter ../filters/pandocfilters/examples/graphviz.py \
  --filter ../filters/pandocfilters/examples/plantuml.py
