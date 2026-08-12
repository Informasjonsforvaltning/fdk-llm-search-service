FROM amazoncorretto:21-alpine

ARG USER=default
ENV HOME=/home/$USER

ENV TZ=Europe/Oslo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# install sudo as root
RUN apk update && apk add --no-cache sudo gcompat
RUN adduser -D $USER && \
      echo "$USER ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/$USER && \
      chmod 0440 /etc/sudoers.d/$USER

USER $USER
WORKDIR $HOME

COPY --chown=$USER:$USER /target/app.jar app.jar

# Run the application
CMD ["sh", "-c", "java -jar -XX:+UseZGC \
         $JAVA_OPTS app.jar"]