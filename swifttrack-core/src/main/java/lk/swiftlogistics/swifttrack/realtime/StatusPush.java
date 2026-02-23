package lk.swiftlogistics.swifttrack.realtime;

import lk.swiftlogistics.swifttrack.domain.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StatusPush {

    private final SimpMessagingTemplate template;

    public StatusPush(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void push(Order o) {
        template.convertAndSend("/topic/orders", o);
    }
}