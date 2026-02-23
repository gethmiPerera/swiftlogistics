package lk.swiftlogistics.swifttrack.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EventPublisher {

    private final RabbitTemplate rabbit;

    public EventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publishOrderCreated(UUID orderId) {
        rabbit.convertAndSend(
            Events.EXCHANGE,
            Events.ORDER_CREATED,
            orderId.toString()
        );
    }
}