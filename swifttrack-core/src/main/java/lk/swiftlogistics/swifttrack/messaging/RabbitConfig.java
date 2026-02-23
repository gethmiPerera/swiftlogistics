package lk.swiftlogistics.swifttrack.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(Events.EXCHANGE);
    }

    @Bean
    Queue queue() {
        return new Queue("order.queue", true);
    }

    @Bean
    Binding binding(Queue q, TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with("#");
    }
}