#!/bin/sh

mkdir -p ~/.m2
cat << EOF > ~/.m2/settings.xml
<mirrors>
    <mirror>
        <id>repo1.maven.org</id>
        <name>repo1.maven.org</name>
        <url>http://repo1.maven.org/maven2</url>
        <mirrorOf>central</mirrorOf>
    </mirror>
    <mirror>
        <id>uk.maven.org</id>
        <name>uk.maven.org</name>
        <url>http://uk.maven.org/maven2</url>
        <mirrorOf>central</mirrorOf>
    </mirror>
    <mirror>
        <id>ibiblio.net</id>
        <url>http://www.ibiblio.net/pub/packages/maven2</url>
    </mirror>
</mirrors>
EOF
