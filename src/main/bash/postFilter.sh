#!/usr/bin/env bash
pandoc \
  --toc \
  --webtex \
  --template=../site-src/templates/pandoc-post.html \
  --filter /root/pandoc-filters/pandocfilters/examples/graphviz.py \
  --filter /root/pandoc-filters/pandocfilters/examples/plantuml.py \
  --filter /root/pandoc-filters/include-code/include-code.py \
