const API = "http://localhost:8080";

// Submit order
function submitOrder() {
    const name = document.getElementById("customerName").value;
    const address = document.getElementById("address").value;

    fetch(API + "/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            customerName: name,
            address: address
        })
    })
    .then(res => res.json())
    .then(data => {
        alert("Order Submitted!");
        loadOrders();
    });
}

// Load orders
function loadOrders() {
    fetch(API + "/api/orders")
        .then(res => res.json())
        .then(data => {
            const div = document.getElementById("orders");
            div.innerHTML = "";

            data.forEach(order => {
                div.innerHTML += `
                    <div class="order-item">
                        Order #${order.id} - 
                        <span class="status-${order.status.toLowerCase()}">
                            ${order.status}
                        </span>
                    </div>
                `;
            });
        });
}
// Mock driver deliveries
function loadDeliveries() {
    const deliveries = [
        { id: 1, address: "Colombo 07", status: "Out for Delivery" },
        { id: 2, address: "Kandy", status: "Pending" }
    ];

    const div = document.getElementById("deliveries");
    if (!div) return;

    div.innerHTML = "";

    deliveries.forEach(d => {
        div.innerHTML += `
            <div class="order-item">
                Order #${d.id} - ${d.address} - ${d.status}
                <br><br>
                <button class="btn-success">Mark Delivered</button>
                <button class="btn-danger">Mark Failed</button>
            </div>
        `;
    });
}

loadDeliveries();

// Tracking mock
function trackOrder() {
    const id = document.getElementById("trackId").value;
    const result = document.getElementById("trackingResult");

    result.innerHTML = `
        <p>Tracking Order #${id}</p>
        <p>✔ Order Created</p>
        <p>✔ Sent to Warehouse</p>
        <p>✔ Route Optimized</p>
        <p>✔ Out for Delivery</p>
        <p class="status-delivered">✔ Delivered</p>
    `;
}

// Auto refresh every 5 seconds (simulate real-time)
setInterval(loadOrders, 5000);