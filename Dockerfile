FROM hseeberger/scala-sbt:8u265_1.4.0_2.13.3 as builder
WORKDIR /app
COPY build.sbt /app/build.sbt
COPY project /app/project
RUN sbt update test:update it:update
COPY . .
RUN sbt stage && \
    chmod -R u=rX,g=rX /app/target/universal/stage && \
    chmod u+x,g+x /app/target/universal/stage/bin/imazsak

FROM openjdk:8-alpine
USER root
RUN apk add --no-cache bash && \
    adduser -S -u 1001 imazsak
USER 1001
EXPOSE 9000
ENTRYPOINT ["/app/bin/imazsak"]
CMD []
COPY --from=builder --chown=1001:root /app/target/universal/stage /app
