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

RUN LEIN_ROOT='yes' lein with-profiles +test,+dev check

RUN curl -O https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN apt-get install libappindicator3-1 lsb-release -y
RUN dpkg -i google-chrome-stable_current_amd64.deb

npm install karma karma-cljs-test karma-chrome-launcher --save-dev

RUN rm -rf /tmp/*
