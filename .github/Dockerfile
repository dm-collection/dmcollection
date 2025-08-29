FROM gcr.io/distroless/java21-debian12:nonroot@sha256:fb0d294a2ba6edffc3776a87dd2dce9771801a3fb0aa7319d51300239dd51aeb
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
