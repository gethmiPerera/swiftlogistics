package lk.swiftlogistics.swifttrack.orchestrator;

import lk.swiftlogistics.swifttrack.domain.Order;
import lk.swiftlogistics.swifttrack.domain.OrderStatus;
import lk.swiftlogistics.swifttrack.integrations.*;
import lk.swiftlogistics.swifttrack.repo.OrderRepository;
import lk.swiftlogistics.swifttrack.realtime.StatusPush;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SagaService {

    private final OrderRepository repo;
    private final CmsSoapClient cms;
    private final WmsTcpClient wms;
    private final RosClient ros;
    private final StatusPush ws;

    public SagaService(OrderRepository repo,
                       CmsSoapClient cms,
                       WmsTcpClient wms,
                       RosClient ros,
                       StatusPush ws) {
        this.repo = repo;
        this.cms = cms;
        this.wms = wms;
        this.ros = ros;
        this.ws = ws;
    }

    public void process(UUID orderId) {
        Order o = repo.findById(orderId).orElseThrow();

        try {
            o.setCmsOrderId(cms.createOrder(orderId.toString()));
            o.setStatus(OrderStatus.CMS_CREATED);
            repo.save(o);
            ws.push(o);

            wms.register(orderId.toString());
            o.setStatus(OrderStatus.WMS_REGISTERED);
            repo.save(o);
            ws.push(o);

            o.setRouteId(ros.planRoute(orderId.toString()));
            o.setStatus(OrderStatus.ROUTE_PLANNED);
            repo.save(o);
            ws.push(o);

            o.setStatus(OrderStatus.OUT_FOR_DELIVERY);
            repo.save(o);
            ws.push(o);

        } catch (Exception e) {
            o.setStatus(OrderStatus.FAILED);
            o.setFailureReason(e.getMessage());
            repo.save(o);
            ws.push(o);
        }
    }
}