package io.quarkuscoffeeshop.kitchen.infrastructure;

import io.quarkuscoffeeshop.domain.EventType;
import io.quarkuscoffeeshop.domain.Item;
import io.quarkuscoffeeshop.domain.OrderInEvent;
import io.quarkuscoffeeshop.domain.OrderUpEvent;
import io.quarkuscoffeeshop.infrastructure.KafkaIT;
import io.quarkuscoffeeshop.infrastructure.KafkaTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class KitchenIT extends KafkaIT {

    Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void testOrderIn() {
        OrderInEvent orderIn = new OrderInEvent(EventType.KITCHEN_ORDER_IN, UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Lemmy", Item.CAKEPOP);
        producerMap.get("kitchen-in").send(new ProducerRecord<>("kitchen-in", jsonb.toJson(orderIn)));

        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            assertNull(e);
        }

        // Get the appropriate consumer, point to the first message, and pull all messages
        final KafkaConsumer baristaConsumer = consumerMap.get("orders");
        baristaConsumer.seekToBeginning(new ArrayList<TopicPartition>());
        final ConsumerRecords<String, String> baristaRecords = baristaConsumer.poll(Duration.ofMillis(1000));

        for (ConsumerRecord<String, String> record : baristaRecords) {
            System.out.println(record.value());
            OrderUpEvent orderUp = jsonb.fromJson(record.value(), OrderUpEvent.class);
            assertEquals(EventType.KITCHEN_ORDER_UP, orderUp.eventType);
            assertEquals("Lemmy", orderUp.name);
            assertEquals(Item.CAKEPOP, orderUp.item);
            assertNotNull(orderUp.madeBy);
        }
    }
}
