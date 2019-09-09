FROM hseeberger/scala-sbt:11.0.3_1.2.8_2.13.0 as builder
WORKDIR /app
COPY build.sbt /app/build.sbt
COPY project /app/project
RUN sbt update test:update it:update
COPY . .
RUN sbt stage && \
    chmod -R u=rX,g=rX /app/target/universal/stage && \
    chmod u+x,g+x /app/target/universal/stage/bin/imazsak


FROM openjdk:11
USER root
RUN useradd --system --create-home --uid 1001 --gid 0 imazsak
USER 1001
EXPOSE 9000
ENTRYPOINT ["/app/bin/imazsak"]
CMD []
COPY --from=builder --chown=1001:root /app/target/universal/stage /app
