FROM gcr.io/distroless/java21-debian12:nonroot@sha256:5fc28363e84dec4db09b7df71299879039639fa9c13db7c9d36ff61715e10859
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
