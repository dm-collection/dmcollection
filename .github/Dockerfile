FROM gcr.io/distroless/java21-debian12:nonroot@sha256:59c3de3664ddf2a69b2accc04ed4650caa578c14e63863075f5d041fe6961369
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
