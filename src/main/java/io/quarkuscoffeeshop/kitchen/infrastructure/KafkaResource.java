package io.quarkuscoffeeshop.kitchen.infrastructure;

import io.quarkuscoffeeshop.domain.*;
import io.quarkuscoffeeshop.kitchen.domain.Kitchen;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.io.StringReader;
import java.util.concurrent.CompletionStage;

public class KafkaResource {

    private static final Logger logger = LoggerFactory.getLogger(KafkaResource.class);

    @Inject
    Kitchen kitchen;

    @Inject @Channel("orders-out")
    Emitter<String> orderUpEmitter;

    final Jsonb jsonb = JsonbBuilder.create();

    @Incoming("orders-in")
    public CompletionStage<Void> handleOrderIn(Message message) {

        logger.debug("message received: {}", message.getPayload());

        String payload = (String) message.getPayload();
        JsonReader jsonReader = Json.createReader(new StringReader(payload));
        JsonObject jsonObject = jsonReader.readObject();

        logger.debug("unmarshalled {}", jsonObject);

        // filter our commands and barista events
        if (jsonObject.containsKey("eventType")) {
            final OrderInEvent orderIn = jsonb.fromJson((String) message.getPayload(), OrderInEvent.class);
            if (orderIn.eventType.equals(EventType.KITCHEN_ORDER_IN)) {
                return kitchen.make(orderIn).thenApply(o -> {
                    logger.debug("sending: {}", o.toString());
                    return orderUpEmitter.send(jsonb.toJson(o));
                }).thenRun( () -> { message.ack(); });
            }else{
                return message.ack();
            }
        }else {
            return message.ack();
        }
    }
}
