package com.example.demo

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class ExampleEventListener {

    private val logger = LoggerFactory.getLogger(ExampleEventListener::class.java)

    @KafkaListener(topics = ["\${example.outbound-topic-name}"])
    fun listenDestination(message: ConsumerRecord<String?, String?>) {
        logger.info("Outbound topic key: ${message.key()}")
        message.headers().forEach { logger.info("Header ${it.key()}: ${String(it.value())}") }
        logger.info("Outbound topic value: ${message.value()}")
    }

    @KafkaListener(topics = ["\${example.inbound-topic-name}"])
    fun listenSource(message: ConsumerRecord<String?, String?>) {
        logger.info("Inbound topic key: ${message.key()}")
        message.headers().forEach { logger.info("The Header ${it.key()}: ${String(it.value())}") }
        logger.info("Inbound topic value: ${message.value()}")
    }
}
