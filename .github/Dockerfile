FROM gcr.io/distroless/java25-debian13:nonroot@sha256:7c1086d2068c2a6eb21dd5fa6440dd80cf13e466a9f34d7cbf96dfe9d897532e
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
