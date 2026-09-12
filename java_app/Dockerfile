FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && mkdir -p /app/libs \
    && curl -fL -o /app/libs/mysql-connector-j.jar \
       https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar \
    && rm -rf /var/lib/apt/lists/*

COPY . /app

RUN mkdir -p /app/out && \
    javac -cp /app/libs/mysql-connector-j.jar -d /app/out $(find /app -maxdepth 1 -name "*.java")

EXPOSE 8083

CMD ["java", "-cp", "/app/out:/app/libs/mysql-connector-j.jar", "testeo"]