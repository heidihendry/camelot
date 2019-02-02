# Docker image for CI

FROM clojure:openjdk-11-lein
MAINTAINER Chris Mann <chris@bitpattern.com.au>

RUN curl -sL https://deb.nodesource.com/setup_10.x | bash -
RUN apt-get update -y
RUN apt-get install nodejs curl firefox-esr -y
RUN mkdir -p ~/bin

RUN curl -O https://download.clojure.org/install/linux-install-1.10.0.411.sh
RUN chmod +x linux-install-1.10.0.411.sh
RUN ./linux-install-1.10.0.411.sh

WORKDIR /tmp
COPY project.clj /tmp/
COPY deps.edn /tmp/
COPY package.json /tmp/
COPY package-lock.json /tmp/

RUN npm install

RUN LEIN_ROOT='yes' lein with-profiles +test,+dev -version

RUN rm -rf /tmp/*
