FROM gcr.io/distroless/java25-debian13:nonroot@sha256:06b29204e9cefa8b6a6919c37397dcf7f557dad53dcd094d5c9394a7c8030c3c
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
