FROM gcr.io/distroless/java21-debian12:nonroot@sha256:dede40c80a5744086f1e0cd961f86b782317ce11a51fc9ba39d9c5d030978900
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
