FROM gcr.io/distroless/java21-debian12:nonroot@sha256:074fdde87f7fc85040d161270f5530109e173e997024fc1e58170eed51b90101
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
