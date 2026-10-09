FROM gcr.io/distroless/java25-debian13:nonroot@sha256:898d73829b4bdebc21fce3c67c09cbca5e8aec50a5611d85b075edce29510dd9
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
