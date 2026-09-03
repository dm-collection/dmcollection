FROM gcr.io/distroless/java25-debian13:nonroot@sha256:9515c0be2f512f5f57c3004df27cd514dfc66a31c0d64fadc8ebb4c6b7a796a9
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
