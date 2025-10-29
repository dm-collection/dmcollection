FROM gcr.io/distroless/java21-debian12:nonroot@sha256:ba82fde7f88ce24611eccea955182020ace3490f5dd16c4ac34ea3b6b9bffddd
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
COPY ./target/server-*.jar app.jar
CMD ["app.jar"]
