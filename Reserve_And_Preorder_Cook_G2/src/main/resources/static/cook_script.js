/**
 * ==========================================================
 * CONFIGURATION
 * ==========================================================
 * Demo-Modus:  true  → erzeugt lokal zufällige Orders
 *               false → lädt echte Orders vom Backend
 * ==========================================================
 */

const demoMode = false; // <<=== hier umschalten

// Backend URLs - Cook App läuft auf 8081
const GET_ORDERS_URL = "http://localhost:8081/api/orders/active";
const POST_DONE_URL  = "http://localhost:8081/api/orders";

let orders = [];

/**
 * ==========================================================
 *  Lädt Orders vom Backend oder Demo-Generator
 * ==========================================================
 */
async function fetchOrders() {
    if (demoMode) {
        generateDemoOrders();
        renderOrders();
        return;
    }

    try {
        const response = await fetch(GET_ORDERS_URL, {
            cache: 'no-cache',
            headers: {
                'Cache-Control': 'no-cache',
                'Pragma': 'no-cache'
            }
        });
        if (!response.ok) throw new Error("Fehler beim Laden");
        const data = await response.json();

        // Convert backend format to frontend format
        orders = Array.isArray(data) ? data.map(order => {
            // Build items list from actual items
            let itemsList = [];
            if (order.items && Array.isArray(order.items) && order.items.length > 0) {
                itemsList = order.items.map(item =>
                    `${item.name} x${item.quantity}`
                );
            } else {
                itemsList = ["Keine Items vorhanden"];
            }

            return {
                orderId: order.id,
                orderTime: order.orderDateTime || new Date().toISOString(),
                tableNumber: order.tableNumber || "?",
                items: itemsList,
                totalPrice: order.totalPrice || "0.00",
                status: order.status === "PENDING" ? "Pending" :
                        order.status === "IN_KITCHEN" ? "In Arbeit" :
                        order.status === "READY" ? "Fertig" : 
                        order.status === "SERVED" ? "Serviert" : order.status,
                deliveryTime: order.deliveryTime || new Date().toISOString(),
                extraInfo: order.specialRequests || ""
            };
        }) : [];

        console.log(`${new Date().toLocaleTimeString()} - Loaded ${orders.length} orders from backend`);
    } catch (err) {
        console.warn("Backend nicht erreichbar:", err.message);
        orders = [];
    }

    sortOrders();
    renderOrders();
}

/**
 * Erstellt eine einzelne Demo-Order
 */
function createDemoOrder(id = "DEMO-" + Math.floor(Math.random() * 9000 + 1000)) {
    const allItems = ["Pizza", "Salat", "Pasta", "Burger", "Pommes", "Wasser", "Kaffee"];
    const items = allItems.sort(() => 0.5 - Math.random()).slice(0, 2);
    return {
        orderId: id,
        orderTime: new Date().toISOString(),
        tableNumber: Math.floor(Math.random() * 10) + 1,
        items: items,
        totalPrice: (Math.random() * 30 + 10).toFixed(2),
        status: "Pending",
        deliveryTime: new Date(Date.now() + (Math.random() * 25 + 5) * 60000).toISOString(),
        extraInfo: "Demo Mode aktiv"
    };
}

/**
 * Wenn Demo-Mode aktiv: fügt jede Minute eine neue zufällige Order hinzu
 */
function generateDemoOrders() {
    // beim ersten Aufruf: mindestens eine Demo-Order
    if (orders.length === 0) {
        orders.push(createDemoOrder());
    }

    // alle 60 Sekunden neue Order hinzufügen
    if (!window.demoIntervalStarted) {
        window.demoIntervalStarted = true;
        setInterval(() => {
            orders.push(createDemoOrder());
            sortOrders();
            renderOrders();
            console.log("Neue Demo-Order hinzugefuegt.");
        }, 60000);
    }

    sortOrders();
}

/**
 * Sortiert Orders nach Bestellzeit (älteste zuerst)
 */
function sortOrders() {
    orders.sort((a, b) => new Date(a.orderTime) - new Date(b.orderTime));
}

/**
 * Rendert alle Orders im Dashboard
 */
function renderOrders() {
    const container = document.getElementById("orders");
    container.innerHTML = "";

    const showDone = document.getElementById("showDone").checked;

    if (!orders.length) {
        container.innerHTML = `<div class="no-orders">Keine Bestellungen vorhanden</div>`;
        return;
    }

    for (const order of orders) {
        if ((order.status === "Fertig" || order.status === "Done") && !showDone) continue;

        const card = document.createElement("div");
        card.className = "order-card";

        // Highlight pending orders
        if (order.status === "Pending") {
            card.classList.add("urgent");
        } else if (order.status === "In Arbeit") {
            card.classList.add("soon");
        }

        const isDone = order.status === "Fertig" || order.status === "Done";

        card.innerHTML = `
      <div class="header">
        <div class="order-id">#${order.orderId}</div>
        <div class="table">Tisch ${order.tableNumber}</div>
      </div>

      <div class="info">
        <p><i class="fas fa-clock"></i> ${new Date(order.orderTime).toLocaleTimeString()}</p>
        <p class="items-list"><i class="fas fa-utensils"></i> ${order.items.join(", ")}</p>
        <p><i class="fas fa-euro-sign"></i> <strong>${Number(order.totalPrice).toFixed(2)}</strong></p>
        ${order.extraInfo ? `<p><i class="fas fa-info-circle"></i> ${order.extraInfo}</p>` : ''}
        <p class="status-line">
          <span class="status-badge status-${order.status.toLowerCase().replace(' ', '-')}">
            ${order.status}
          </span>
        </p>
      </div>

      ${!isDone ?
            `<button class="btn" onclick="markAsDone('${order.orderId}')">
              <i class="fas fa-check"></i> Fertig
            </button>` :
            `<div class="done">An Kellner uebergeben</div>`
        }
    `;
        container.appendChild(card);
    }
}

/**
 * Klick auf "Fertig" sendet POST an Backend oder markiert lokal
 */
async function markAsDone(orderId) {
    const order = orders.find(o => String(o.orderId) === String(orderId));
    if (!order) return;

    order.status = "Fertig";
    renderOrders();

    if (demoMode) {
        console.log(`Demo Mode: Order ${orderId} als fertig markiert (nicht gesendet).`);
        return;
    }

    try {
        const res = await fetch(`${POST_DONE_URL}/${orderId}/ready`, {
            method: "POST",
            headers: { "Content-Type": "application/json" }
        });
        if (!res.ok) throw new Error(await res.text());
        console.log(`Order ${orderId} marked as READY (fertig zum Servieren)`);
    } catch (err) {
        console.error("Fehler beim HTTP-POST:", err.message);
    }
}

// Automatische Aktualisierung alle 5 Sekunden
if (!demoMode) {
    setInterval(fetchOrders, 5000);
    console.log("Auto-refresh aktiviert: alle 5 Sekunden");
}

// Initialer Start
fetchOrders();
