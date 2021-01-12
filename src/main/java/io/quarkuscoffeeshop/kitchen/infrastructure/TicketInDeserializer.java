package io.quarkuscoffeeshop.kitchen.infrastructure;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkuscoffeeshop.kitchen.domain.valueobjects.TicketIn;

public class TicketInDeserializer extends ObjectMapperDeserializer<TicketIn> {

    public TicketInDeserializer() {
        super(TicketIn.class);
    }
}
