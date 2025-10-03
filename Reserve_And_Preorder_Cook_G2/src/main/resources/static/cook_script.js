let orders = [];

// Dummy Orders jede Minute hinzufügen
setInterval(() => {
    const id = Math.floor(Math.random() * 10000);
    receiveOrder({
        orderTime: new Date().toISOString(),
        orderId: id.toString(),
        tableNumber: Math.ceil(Math.random() * 10),
        items: ["Pizza", "Salad", "Water"].sort(() => 0.5 - Math.random()).slice(0,2),
        totalPrice: (Math.random() * 30 + 10).toFixed(2),
        status: "Pending",
        deliveryTime: new Date(Date.now() + (Math.random() * 25 + 5) * 60000).toISOString(),
        extraInfo: "Allergy: None"
    });
}, 60000);

// Startorder zum Test
receiveOrder({
    orderTime: new Date().toISOString(),
    orderId: "100",
    tableNumber: 4,
    items: ["Burger", "Fries"],
    totalPrice: 15.90,
    status: "Pending",
    deliveryTime: new Date(Date.now() + 12 * 60000).toISOString(),
    extraInfo: "No onions please"
});

function receiveOrder(orderJson) {
    orders.push(orderJson);
    sortOrders();
    renderOrders();
}

function sortOrders() {
    orders.sort((a, b) => new Date(a.deliveryTime) - new Date(b.deliveryTime));
}

function renderOrders() {
    const container = document.getElementById("orders");
    container.innerHTML = "";
    const showDone = document.getElementById("showDone").checked;

    orders.forEach(order => {
        if (order.status === "Done" && !showDone) return;

        const div = document.createElement("div");
        div.className = "order";
        div.id = `order-${order.orderId}`;

        const timeLeft = (new Date(order.deliveryTime) - new Date()) / 1000 / 60;
        if (timeLeft <= 5 && order.status === "Pending") {
            div.classList.add("blinking-fast");
        } else if (timeLeft <= 15 && order.status === "Pending") {
            div.classList.add("blinking");
        }

        div.innerHTML = `
            <h3>Order #${order.orderId} (Table ${order.tableNumber})</h3>
            <div class="info-row"><i class="fas fa-clock"></i> ${new Date(order.deliveryTime).toLocaleTimeString()}</div>
            <div class="info-row"><i class="fas fa-utensils"></i> ${order.items.join(", ")}</div>
            <div class="info-row"><i class="fas fa-euro-sign"></i> €${order.totalPrice}</div>
            <div class="info-row"><i class="fas fa-info-circle"></i> Status: ${order.status}</div>
            <div class="extra-info"><i class="fas fa-sticky-note"></i> ${order.extraInfo || "No extra info"}</div>
            ${order.status !== "Done" ? `<button onclick="markAsDone('${order.orderId}')">Mark as Done</button>` : ""}
        `;

        container.appendChild(div);
    });
}

function markAsDone(orderId) {
    const order = orders.find(o => o.orderId === orderId);
    if (order) {
        order.status = "Done";
        renderOrders();
        console.log(`Order ${orderId} marked as done.`);
    }
}

// Damit Blinken dynamisch bleibt
setInterval(renderOrders, 30000);
