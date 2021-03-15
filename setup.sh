curl https://repo.maven.apache.org/maven2/org/apache/camel/kafkaconnector/camel-kafka-kafka-connector/0.8.0/camel-kafka-kafka-connector-0.8.0-package.tar.gz --output camel-kafka-kafka-connector-0.8.0-package.tar.gz

mkdir connectors

tar -C ./connectors -zxvf camel-kafka-kafka-connector-0.8.0-package.tar.gz

rm camel-kafka-kafka-connector-0.8.0-package.tar.gz
