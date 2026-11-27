FROM gcr.io/distroless/java25-debian13:nonroot@sha256:6f372d8e28092f93b3227ba3f530d3cf7d67422569b24a35543a14fcbb9749e5
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
