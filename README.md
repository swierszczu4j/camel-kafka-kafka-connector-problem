# Camel Kafka Kafka Connector Problem Showcase

Simple project showcasing the
[Camel Kafka Kafka Connector issue](https://github.com/apache/camel-kafka-connector/issues/988).
The problem is, that the connector is not passing forward the message key that comes from the inbound topic.

## Intro

This project contains simple Spring Boot app with ShowcaseTest, which is currently failing expecting the connector not
to lose the message key in between. You can also see exactly the same behavior using prepared docker-compose file, and
while running the DemoApplication, in the logs you will see that on the outbound topic the key is missing

## How to set it up?

1. Run `setup.sh` - downloads the connector and places it in connectors folder.
2. Run `docker-compose up -d` - to make the whole infra work.
3. Run `register-camel-connector.sh` - registers the Camel Kafka Kafka Connector.
4. Build app using `mvn clean package -DskipTests`.
5. Run the app using `mvn spring-boot:run`.
6. Run `sample-request.sh` to see the issue in the console.

OR

Build the project with `mvn clean package` and see that the ShowcaseTest is failing due to missing key in outbound topic.
