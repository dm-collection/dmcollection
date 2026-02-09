FROM gcr.io/distroless/java25-debian13:nonroot@sha256:4e917e064be1472cb05551ef106210d5c527e4de30151e97e07f8cb7a19d089f
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
