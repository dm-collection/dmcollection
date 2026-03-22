FROM gcr.io/distroless/java25-debian13:nonroot@sha256:5904cdb2fb56ba764806d44d2d48f768d1be7510b15ae9abed0f8239600cee68
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
