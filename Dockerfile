FROM docker.io/library/node:24-alpine AS ui-builder
WORKDIR /app
COPY ui/package.json ui/package-lock.json ./
RUN npm ci
COPY ui/ .
RUN npm run build

FROM docker.io/library/maven:3.9-sapmachine-25 AS server-builder
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
COPY server/pom.xml server/
RUN ./mvnw -B dependency:go-offline -pl server
COPY --from=ui-builder /app/build/ server/src/main/resources/static/
COPY server/src/ server/src/
RUN ./mvnw -B package -DskipTests -pl server

FROM gcr.io/distroless/java25-debian13:nonroot@sha256:ed086fc864ff98def5c74ebfe5f00c61e02cc52668469ac7b3791bac02e1cf16
ARG APP_USER_HOME=/home/nonroot
WORKDIR ${APP_USER_HOME}
ENV JDK_JAVA_OPTIONS="-XX:+UseCompactObjectHeaders"
COPY --from=server-builder /app/server/target/server-*.jar app.jar
CMD ["app.jar"]
