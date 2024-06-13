FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine
RUN mkdir /app
COPY build/libs/auth-1.0.jar /app
CMD ["java", "-jar", "/app/auth-1.0.jar"]
#CMD ["java", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-Xss256k", "-jar", "/app/auth-1.0.jar"]
#CMD ["java", "-XX:+UseParallelGC", "-jar", "/app/auth-1.0.jar"]
#CMD ["java", "-XX:+UseSerialGC", "-jar", "/app/auth-1.0.jar"]
#CMD ["java", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/storage/data/dumps", "-XX:+FlightRecorder", "-XX:StartFlightRecording=disk=true,filename=/storage/data/dumps/recording.jfr,maxage=24h,dumponexit=true,path-to-gc-roots=true", "-jar", "/app/auth-1.0.jar"]
