FROM gcr.io/distroless/java25-debian13:nonroot@sha256:08377e3cacfd8b4245a891bd77fd54a40af55ed90695a1007be19b0c7a36a812
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
