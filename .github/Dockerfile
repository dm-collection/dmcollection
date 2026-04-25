FROM gcr.io/distroless/java25-debian13:nonroot@sha256:ed086fc864ff98def5c74ebfe5f00c61e02cc52668469ac7b3791bac02e1cf16
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
