FROM hseeberger/scala-sbt

RUN apt-get update
RUN apt-get -y upgrade
RUN apt-get install -y\
  pandoc python-pip plantuml graphviz\
  libgraphviz-dev graphviz-dev pkg-config

RUN pip install pandocfilters pygraphviz

WORKDIR /root/thera
CMD ./serve.sh
