FROM hseeberger/scala-sbt

WORKDIR /root/pandoc-filters

# PlantUML and GraphVIZ Python plugins
RUN apt-get update
RUN apt-get -y upgrade
RUN apt-get install -y\
  pandoc python-pip plantuml graphviz\
  libgraphviz-dev graphviz-dev pkg-config
RUN pip install pandocfilters pygraphviz
RUN git clone https://github.com/anatoliykmetyuk/pandocfilters.git

# Code-include Haskell dependencies
ENV PATH=$PATH:/root/.local/bin
RUN curl -sSL https://get.haskellstack.org/ | sh
RUN apt-get install -y libghc-pcre-light-dev
RUN git clone https://github.com/anatoliykmetyuk/pandoc-include-code.git
WORKDIR /root/pandoc-filters/pandoc-include-code
RUN stack install

# Start a server to browse the generated site
WORKDIR /root/thera
CMD ./serve.sh
