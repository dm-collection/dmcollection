FROM gcr.io/distroless/java25-debian13:nonroot@sha256:7c7cdac86fc55ba2d4db46bd932e836f53ab0ac1a8533b6b454edb98fd34e78e
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
