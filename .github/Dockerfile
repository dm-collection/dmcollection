FROM gcr.io/distroless/java25-debian13:nonroot@sha256:4e9260c5005f9f0722b11e64e608ddd787d743342e80c068bab7d9c5c7204a90
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
