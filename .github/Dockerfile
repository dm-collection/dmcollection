FROM gcr.io/distroless/java25-debian13:nonroot@sha256:6ca6f13fb89004e4dc8431ad5b9100cf608992d57efa6dae2e513258b5d64ec0
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
