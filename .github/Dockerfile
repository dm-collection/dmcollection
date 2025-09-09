FROM gcr.io/distroless/java21-debian12:nonroot@sha256:dfea876744f635b5fea3ea53099410cfc5d17aeea7b62887310512d78094f6f3
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
