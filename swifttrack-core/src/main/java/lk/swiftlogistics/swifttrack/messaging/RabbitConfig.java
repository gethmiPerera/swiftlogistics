package lk.swiftlogistics.swifttrack.messaging;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /* ---- Main exchange ---- */
    @Bean
    TopicExchange exchange() {
        return new TopicExchange(Events.EXCHANGE);
    }

    /* ---- Dead-letter exchange (DLX) ---- */
    @Bean
    DirectExchange dlxExchange() {
        return new DirectExchange("order.exchange.dlx");
    }

    /* ---- Main queue — routes failed messages to the retry queue via DLX ---- */
    @Bean
    Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
                .withArgument("x-dead-letter-exchange", "order.exchange.dlx")
                .withArgument("x-dead-letter-routing-key", "order.retry")
                .build();
    }

    /* ---- Retry queue — holds failed messages for 5 s, then re-publishes to main exchange ---- */
    @Bean
    Queue retryQueue() {
        return QueueBuilder.durable("order.queue.retry")
                .withArgument("x-dead-letter-exchange", Events.EXCHANGE)
                .withArgument("x-dead-letter-routing-key", Events.ORDER_CREATED)
                .withArgument("x-message-ttl", 5000)
                .build();
    }

    /* ---- Dead-letter queue (DLQ) — final resting place for poison messages ---- */
    @Bean
    Queue dlq() {
        return QueueBuilder.durable("order.queue.dlq").build();
    }

    /* ---- Bindings ---- */
    @Bean
    Binding mainBinding(@Qualifier("orderQueue") Queue orderQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderQueue).to(exchange).with("#");
    }

    @Bean
    Binding retryBinding(@Qualifier("retryQueue") Queue retryQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(retryQueue).to(dlxExchange).with("order.retry");
    }

    @Bean
    Binding dlqBinding(@Qualifier("dlq") Queue dlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlq).to(dlxExchange).with("order.dlq");
    }
}