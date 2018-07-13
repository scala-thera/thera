#!/usr/bin/env bash

echo -n $1 | pandoc \
  --toc \
  --webtex \
  --template=tml.html \
  --filter src/main/python/graphviz.py \
  --filter src/main/python/plantuml.py
