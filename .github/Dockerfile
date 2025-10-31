FROM gcr.io/distroless/java21-debian12:nonroot@sha256:5d281651dbf431f0fa9bc28bce5514a5594d66ccd2f9bf8602adccfc2518d4f9
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
