package com.example.demo

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
class ExampleController(
        private val kafkaTemplate: KafkaTemplate<String, String>,
        private val objectMapper: ObjectMapper,
        @Value("\${example.inbound-topic-name}")
        private val inboundExampleTopicName: String
) {

    @PostMapping("/examples")
    fun addExample(@RequestBody event: ExampleEvent) {
        kafkaTemplate.send(inboundExampleTopicName, event.exampleId.toString(), objectMapper.writeValueAsString(event)).get(5, TimeUnit.SECONDS)
    }

}
