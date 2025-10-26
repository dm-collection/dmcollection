FROM gcr.io/distroless/java21-debian12:nonroot@sha256:610f68e186d9a190d13d19d47ffa379daf63035fb5b80e17d87d8e44074f5dad
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
