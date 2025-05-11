FROM gcr.io/distroless/java21-debian12:nonroot@sha256:a4087689e6bda03258bf1ee4dffe2a86383bc8051eaf2fcf72bdeae5815a6e2d
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
