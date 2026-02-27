FROM gcr.io/distroless/java25-debian13:nonroot@sha256:d66268d76ebccab22698b267508e3d2e9d7558ba10abccdd7b1f6e0894f3654c
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
