FROM hseeberger/scala-sbt

RUN apt-get update
RUN apt-get -y upgrade

RUN apt-get install -y\
  pandoc python-pip plantuml graphviz\
  libgraphviz-dev graphviz-dev pkg-config
RUN pip install pandocfilters pygraphviz
RUN sh -c '(echo "#!/usr/bin/env sh" && curl -L https://github.com/lihaoyi/Ammonite/releases/download/1.1.2/2.12-1.1.2) > /usr/local/bin/amm && chmod +x /usr/local/bin/amm'

# ARG CACHE_DATE=not_a_date
WORKDIR /pandoc-filters
RUN git clone https://github.com/anatoliykmetyuk/pandocfilters.git
RUN git clone https://github.com/anatoliykmetyuk/include-code.git

# Start a server to browse the generated site
WORKDIR /root/thera
CMD (mkdir _site; cd _site && python -m SimpleHTTPServer 8888)
