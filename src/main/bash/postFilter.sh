#!/usr/bin/env bash
pandoc \
  --toc \
  --webtex \
  --template=../site-src/templates/pandoc-post.html \
  --filter ../src/main/python/graphviz.py \
  --filter ../src/main/python/plantuml.py
