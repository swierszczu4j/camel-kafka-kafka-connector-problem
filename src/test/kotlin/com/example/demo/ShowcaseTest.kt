package com.example.demo

import com.fasterxml.jackson.databind.ObjectMapper
import io.debezium.testing.testcontainers.Connector
import io.debezium.testing.testcontainers.DebeziumContainer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestConstructor
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.*
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@SpringBootTest(
        classes = [
            DemoApplication::class
        ],
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class ShowcaseTest(
        private val objectMapper: ObjectMapper,
        @Value("\${example.inbound-topic-name}")
        private val inboundExampleTopicName: String,
        @Value("\${example.outbound-topic-name}")
        private val outboundExampleTopicName: String
) {

    private val restTemplate = RestTemplate()

    @BeforeEach
    fun setupConnector() {
        val connectorConfig = """
            {
              "name": "camel-showcase-connector",
              "config": {
                "connector.class": "org.apache.camel.kafkaconnector.kafka.CamelKafkaSinkConnector",
                "tasks.max": "1",
                "camel.sink.path.topic": "$outboundExampleTopicName",
                "camel.sink.endpoint.brokers": "kafka:9092",
                "topics": "$inboundExampleTopicName",
                "key.converter": "org.apache.kafka.connect.storage.StringConverter",
                "value.converter": "org.apache.kafka.connect.storage.StringConverter"
              }
            }
        """.trimIndent()
        registerConnector(connectorConfig)
    }

    @Test
    fun `should receive event on inbound & outbound topics with same key-value pair`() {
        val exampleId = randomUUID()
        val exampleEvent = ExampleEvent(exampleId, "Example added")

        restTemplate.postForLocation("http://localhost:8080/examples", exampleEvent)

        getConsumerForKafkaContainer().use { consumer ->

            consumer.subscribe(listOf(inboundExampleTopicName))
            val inboundRecord = KafkaTestUtils.getSingleRecord(consumer, inboundExampleTopicName, TimeUnit.SECONDS.toMillis(10))
            assertThat(inboundRecord.key()).isEqualTo(exampleId)
            JSONAssert.assertEquals(objectMapper.writeValueAsString(exampleEvent), inboundRecord.value(), JSONCompareMode.LENIENT)
            consumer.unsubscribe()

            consumer.subscribe(listOf(outboundExampleTopicName))
            val outboundRecord = KafkaTestUtils.getSingleRecord(consumer, outboundExampleTopicName, TimeUnit.SECONDS.toMillis(10))
            assertThat(outboundRecord.key())
                    .isNotNull() //here's the actual problem - camel connector is losing message key in between
                    .isEqualTo(exampleId)
            JSONAssert.assertEquals(objectMapper.writeValueAsString(exampleEvent), outboundRecord.value(), JSONCompareMode.LENIENT)
            consumer.unsubscribe()
        }
    }

    private fun getConsumerForKafkaContainer(): KafkaConsumer<UUID, String> {
        return KafkaConsumer(
                mapOf<String, Any>(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG to "test-group-${randomUUID()}",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
                ),
                UUIDDeserializer(),
                StringDeserializer())
    }

    private fun registerConnector(json: String) {
        val connector = Connector.fromJson(ByteArrayInputStream(json.toByteArray(Charset.forName("UTF-8"))))
        registerConnector(connector.toJson(), kafkaConnectContainer.connectorsURI)
        kafkaConnectContainer.ensureConnectorState(connector.name, Connector.State.RUNNING)
    }

    private fun registerConnector(payload: String, fullUrl: String) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        if (!restTemplate.postForEntity(fullUrl, HttpEntity<String>(payload, headers), Any::class.java).statusCode.is2xxSuccessful) {
            throw IllegalStateException("Connector not registered!")
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(ShowcaseTest::class.java)

        @BeforeAll
        @JvmStatic
        fun setup() {
            Startables.deepStart(Stream.of(kafkaContainer, kafkaConnectContainer)).join()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
            registry.apply {
                add("spring.kafka.bootstrap-servers") { kafkaContainer.bootstrapServers }
            }
        }

        private val network = Network.newNetwork()

        private val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.0.0"))
                .withNetworkAliases("kafka")
                .withNetwork(network)

        private val kafkaConnectContainer = DebeziumContainer.latestStable()
                .withNetwork(network)
                .withKafka(kafkaContainer)
                .withLogConsumer(Slf4jLogConsumer(logger))
                .dependsOn(kafkaContainer)
                .withFileSystemBind("connectors/camel-kafka-kafka-connector", "/kafka/connect/camel-kafka-kafka-connector", BindMode.READ_ONLY)
    }

}
