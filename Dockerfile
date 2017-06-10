# Docker image for CI

FROM clojure
MAINTAINER Chris Mann <chris@bitpattern.com.au>

RUN apt-get update -y
RUN apt-get install nodejs nodejs-legacy npm curl -y
RUN npm install -g phantomjs-prebuilt
RUN mkdir -p ~/bin
RUN curl -fsSLo ~/bin/boot https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh
RUN chmod 755 ~/bin/boot

WORKDIR /tmp
COPY build.boot boot.properties /tmp/
COPY src /tmp/src/
COPY resources /tmp/resources/

RUN BOOT_AS_ROOT='yes' ~/bin/boot show -d

RUN rm -rf /tmp/*
