/**
 * ==========================================================
 * CONFIGURATION
 * ==========================================================
 * Demo-Modus:  true  → erzeugt lokal zufällige Orders
 *               false → lädt echte Orders vom Backend
 * ==========================================================
 */

const demoMode = true; // <<=== hier umschalten

// Backend URLs
const GET_ORDERS_URL = "http://localhost:8080/api/orders";
const POST_DONE_URL  = "http://localhost:8080/api/orders";

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
        const response = await fetch(GET_ORDERS_URL);
        if (!response.ok) throw new Error("Fehler beim Laden");
        const data = await response.json();
        orders = Array.isArray(data) ? data : [];

        // Wenn keine Orders im Backend, füge Dummy hinzu
        if (orders.length === 0) {
            console.log("Keine Orders im Backend – füge Demo-Order hinzu");
            orders.push(createDemoOrder("DEMO-1"));
        }
    } catch (err) {
        console.warn("Backend nicht erreichbar – Demo-Daten aktiv");
        orders = [createDemoOrder("DEMO-LOCAL")];
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
            console.log("🧪 Neue Demo-Order hinzugefügt.");
        }, 60000);
    }

    sortOrders();
}

/**
 * Sortiert Orders nach Lieferzeit (nächste zuerst)
 */
function sortOrders() {
    orders.sort((a, b) => new Date(a.deliveryTime) - new Date(b.deliveryTime));
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
        if (order.status === "Done" && !showDone) continue;

        const card = document.createElement("div");
        card.className = "order-card";

        const minutesLeft = (new Date(order.deliveryTime) - new Date()) / 60000;
        if (minutesLeft <= 5 && order.status === "Pending") card.classList.add("urgent");
        else if (minutesLeft <= 15 && order.status === "Pending") card.classList.add("soon");

        card.innerHTML = `
      <div class="header">
        <div class="order-id">#${order.orderId}</div>
        <div class="table">Tisch ${order.tableNumber}</div>
      </div>

      <div class="info">
        <p><i class="fas fa-clock"></i> ${new Date(order.deliveryTime).toLocaleTimeString()}</p>
        <p><i class="fas fa-utensils"></i> ${order.items.join(", ")}</p>
        <p><i class="fas fa-euro-sign"></i> €${Number(order.totalPrice).toFixed(2)}</p>
        <p><i class="fas fa-info-circle"></i> ${order.extraInfo || "Keine Zusatzinfo"}</p>
        <p><i class="fas fa-tag"></i> Status: ${order.status}</p>
      </div>

      ${order.status !== "Done" ?
            `<button class="btn" onclick="markAsDone('${order.orderId}')">Erledigt</button>` :
            `<div class="done">✔️ Erledigt</div>`
        }
    `;
        container.appendChild(card);
    }
}

/**
 * Klick auf "Erledigt" sendet POST an Backend oder markiert lokal
 */
async function markAsDone(orderId) {
    const order = orders.find(o => o.orderId === orderId);
    if (!order) return;

    order.status = "Done";
    renderOrders();

    if (demoMode) {
        console.log(`Demo Mode: Order ${orderId} als erledigt markiert (nicht gesendet).`);
        return;
    }

    try {
        const res = await fetch(`${POST_DONE_URL}/${orderId}/done`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status: "Done" })
        });
        if (!res.ok) throw new Error(await res.text());
        console.log(`Order ${orderId} erfolgreich gesendet an ${POST_DONE_URL}/${orderId}/done`);
    } catch (err) {
        console.error("Fehler beim HTTP-POST:", err.message);
    }
}

// Automatische Aktualisierung alle 60 Sekunden (nur wenn kein Demo-Modus)
if (!demoMode) setInterval(fetchOrders, 60000);

// Initialer Start
fetchOrders();
