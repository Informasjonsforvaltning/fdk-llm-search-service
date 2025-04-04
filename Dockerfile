FROM amazoncorretto:21-alpine3.18

ARG USER=default
ENV HOME=/home/$USER

ENV TZ=Europe/Oslo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# install sudo as root
RUN apk update && apk add --no-cache sudo java-snappy
RUN adduser -D $USER && \
      echo "$USER ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/$USER && \
      chmod 0440 /etc/sudoers.d/$USER

USER $USER
WORKDIR $HOME

COPY --chown=$USER:$USER /target/app.jar app.jar

# Run the application
CMD ["sh", "-c", "java -jar -XX:+UseZGC \
         -Dorg.xerial.snappy.use.systemlib=true \
         -Dorg.xerial.snappy.lib.path=/usr/lib/libsnappy.so.1 \
         $JAVA_OPTS app.jar"]