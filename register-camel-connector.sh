curl -X PUT 'http://localhost:8083/connectors/camel-showcase-connector/config' \
-H 'Content-Type: application/json' \
-d '{
                "connector.class": "org.apache.camel.kafkaconnector.kafka.CamelKafkaSinkConnector",
                "tasks.max": "1",
                "camel.sink.path.topic": "outbound.example.topic",
                "camel.sink.endpoint.brokers": "kafka-1:9092,kafka-2:9092,kafka-3:9092",
                "topics": "inbound.example.topic",
                "key.converter": "org.apache.kafka.connect.storage.StringConverter",
                "value.converter": "org.apache.kafka.connect.storage.StringConverter"
              }'
