FROM openjdk:8u171-jre-slim-stretch

COPY ./target/load.jar /load.jar

CMD java -jar /load.jar