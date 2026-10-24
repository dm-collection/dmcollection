FROM gcr.io/distroless/java25-debian13:nonroot@sha256:4dda92b74e23875e2099186901d54565c3f7c66c896b5d3f04108f1936b973ee
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
