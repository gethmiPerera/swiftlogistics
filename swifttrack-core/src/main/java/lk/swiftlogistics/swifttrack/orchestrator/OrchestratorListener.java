package lk.swiftlogistics.swifttrack.orchestrator;

import lk.swiftlogistics.swifttrack.messaging.Events;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrchestratorListener {

    private final SagaService saga;

    public OrchestratorListener(SagaService saga) {
        this.saga = saga;
    }

    @RabbitListener(queues = "order.queue")
    public void onMessage(String orderId) {
        saga.process(UUID.fromString(orderId));
    }
}