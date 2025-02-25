FROM gradle:jdk21-alpine

# Make directory for processing
RUN mkdir /Processor_specifications_collector
WORKDIR /Processor_specifications_collector

# COPY Folders
ADD app app
ADD gradle gradle
ADD specifications_out specifications_out

# Copy Files
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle .
COPY LICENSE .
COPY README.md .

RUN ls /Processor_specifications_collector/app/snapshots/Intel/processor_family_urls

# Install Chromium (for web scraping)
RUN echo "http://dl-cdn.alpinelinux.org/alpine/edge/community" > /etc/apk/repositories
RUN echo "http://dl-cdn.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories
RUN apk update && apk add  firefox

RUN ls -ls /usr/lib
RUN ls -ls /usr/bin
RUN ls -ls /usr/lib/firefox

RUN chmod -R 777 /Processor_specifications_collector

# Shell as entrypoint
ENTRYPOINT [ "/bin/sh" ]

# Gradle run as command on execution
CMD [ "./gradlew", "run"]