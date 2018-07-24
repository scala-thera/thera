FROM hseeberger/scala-sbt

WORKDIR /root/pandoc-filters

RUN apt-get update
RUN apt-get -y upgrade

RUN apt-get install -y\
  pandoc python-pip plantuml graphviz\
  libgraphviz-dev graphviz-dev pkg-config
RUN pip install pandocfilters pygraphviz
RUN git clone https://github.com/anatoliykmetyuk/pandocfilters.git
RUN git clone https://github.com/anatoliykmetyuk/include-code.git

# Start a server to browse the generated site
WORKDIR /root/thera
CMD ./serve.sh
