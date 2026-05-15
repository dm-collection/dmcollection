FROM gcr.io/distroless/java25-debian13:nonroot@sha256:fd1350d20f28d4f545298700416fd9a12711c572adf651f65f42b3393455d3a6
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
