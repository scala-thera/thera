#!/usr/bin/env bash
pandoc \
  --toc \
  --webtex \
  --template=../site-src/templates/pandoc-post.html \
  --filter /root/filters/pandocfilters/examples/graphviz.py \
  --filter /root/filters/pandocfilters/examples/plantuml.py
