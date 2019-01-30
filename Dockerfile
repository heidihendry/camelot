# Docker image for CI

FROM clojure:openjdk-11-lein
MAINTAINER Chris Mann <chris@bitpattern.com.au>

RUN apt-get update -y
RUN apt-get install nodejs nodejs-legacy curl chromium -y
RUN mkdir -p ~/bin

RUN curl -O https://download.clojure.org/install/linux-install-1.10.0.411.sh
RUN chmod +x linux-install-1.10.0.411.sh
RUN ./linux-install-1.10.0.411.sh

WORKDIR /tmp
COPY project.clj /tmp/
COPY deps.edn /tmp/
COPY src /tmp/src/
COPY resources /tmp/resources/
COPY script /tmp/script
COPY test /tmp/test

RUN LEIN_ROOT='yes' lein check

RUN rm -rf /tmp/*
