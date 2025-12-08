FROM gcr.io/distroless/java21-debian12:nonroot@sha256:2491db610eb845917845285aa7bac120aa8a16d85435d03c8452d5ab24c1bf79
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
