package lk.swiftlogistics.swifttrack.repo;

import lk.swiftlogistics.swifttrack.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}