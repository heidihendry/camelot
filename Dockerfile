# Docker image for CI

FROM clojure
MAINTAINER Chris Mann <chris@bitpattern.com.au>

RUN apt-get update -y
RUN apt-get install nodejs nodejs-legacy npm curl -y
RUN mkdir -p ~/bin
RUN apt-get update -y
RUN apt-get install chromium -y
RUN curl -fsSLo ~/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
RUN chmod 755 ~/bin/lein

WORKDIR /tmp
COPY project.clj /tmp/
COPY src /tmp/src/
COPY resources /tmp/resources/
COPY script /tmp/script
COPY test /tmp/test

RUN LEIN_ROOT='yes' ~/bin/lein check

COPY figwheel-main.edn /tmp/figwheel-main.edn
RUN script/run-tests.sh cljs

RUN rm -rf /tmp/*
