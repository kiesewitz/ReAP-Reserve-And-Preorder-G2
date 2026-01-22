// Globale Variablen
let cart = [];
let reservationId = null;
let restaurantId = null;
let reservationDateTime = null;

// API URLs
const OWNER_API = 'http://localhost:8083/api';
const COOK_API = 'http://localhost:8081/api';

// URL-Parameter auslesen
function getUrlParams() {
    const params = new URLSearchParams(window.location.search);
    reservationId = params.get('reservationId');
    restaurantId = params.get('restaurantId');
    reservationDateTime = params.get('datetime');

    // Reservierungs-Info anzeigen
    if (reservationDateTime) {
        const date = new Date(reservationDateTime);
        document.getElementById('reservationDate').textContent =
            date.toLocaleDateString('de-DE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
        document.getElementById('reservationTime').textContent =
            date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' }) + ' Uhr';
    }

    const guests = params.get('guests');
    if (guests) {
        document.getElementById('reservationGuests').textContent = guests + ' Gäste';
    }
}

// Speisekarte vom Owner-Modul laden
async function loadMenu() {
    if (!restaurantId) {
        restaurantId = 1; // Default Restaurant
    }

    try {
        const response = await fetch(`${OWNER_API}/menu/${restaurantId}/available`);
        if (!response.ok) {
            throw new Error('Speisekarte konnte nicht geladen werden');
        }
        const menuItems = await response.json();
        renderMenu(menuItems);
    } catch (error) {
        console.error('Fehler beim Laden der Speisekarte:', error);
        document.getElementById('menuContainer').innerHTML =
            '<p class="error-text">Fehler beim Laden der Speisekarte. Bitte versuchen Sie es später erneut.</p>';
    }
}

// Speisekarte nach Kategorien rendern
function renderMenu(menuItems) {
    const container = document.getElementById('menuContainer');
    container.innerHTML = '';

    // Nach Kategorien gruppieren
    const categories = {};
    const categoryOrder = ['Vorspeise', 'Hauptgericht', 'Dessert', 'Getränk'];

    menuItems.forEach(item => {
        const category = item.category || 'Sonstiges';
        if (!categories[category]) {
            categories[category] = [];
        }
        categories[category].push(item);
    });

    // Kategorien in definierter Reihenfolge rendern
    categoryOrder.forEach(categoryName => {
        if (categories[categoryName] && categories[categoryName].length > 0) {
            const categorySection = createCategorySection(categoryName, categories[categoryName]);
            container.appendChild(categorySection);
        }
    });

    // Restliche Kategorien (falls vorhanden)
    Object.keys(categories).forEach(categoryName => {
        if (!categoryOrder.includes(categoryName)) {
            const categorySection = createCategorySection(categoryName, categories[categoryName]);
            container.appendChild(categorySection);
        }
    });
}

// Kategorie-Sektion erstellen
function createCategorySection(categoryName, items) {
    const section = document.createElement('div');
    section.className = 'menu-category';

    const header = document.createElement('h3');
    header.className = 'category-header';
    header.textContent = getCategoryDisplayName(categoryName);
    section.appendChild(header);

    const grid = document.createElement('div');
    grid.className = 'menu-grid';

    items.forEach(item => {
        const card = createMenuItemCard(item);
        grid.appendChild(card);
    });

    section.appendChild(grid);
    return section;
}

// Kategorie-Anzeigenamen
function getCategoryDisplayName(category) {
    const names = {
        'Vorspeise': 'Vorspeisen',
        'Hauptgericht': 'Hauptgerichte',
        'Dessert': 'Desserts',
        'Getränk': 'Getränke'
    };
    return names[category] || category;
}

// Menü-Item Karte erstellen
function createMenuItemCard(item) {
    const card = document.createElement('div');
    card.className = 'menu-item-card';
    card.dataset.itemId = item.id;

    card.innerHTML = `
        <div class="menu-item-info">
            <h4 class="menu-item-name">${item.name}</h4>
            <p class="menu-item-description">${item.description || ''}</p>
            <p class="menu-item-price">${formatPrice(item.price)} €</p>
        </div>
        <div class="menu-item-actions">
            <div class="quantity-selector">
                <button class="qty-btn minus" onclick="decreaseQuantity(${item.id})">-</button>
                <span class="qty-display" id="qty-${item.id}">0</span>
                <button class="qty-btn plus" onclick="increaseQuantity(${item.id}, '${escapeQuotes(item.name)}', ${item.price})">+</button>
            </div>
        </div>
    `;

    return card;
}

// Anführungszeichen escapen für onclick
function escapeQuotes(str) {
    return str.replace(/'/g, "\\'").replace(/"/g, '\\"');
}

// Preis formatieren
function formatPrice(price) {
    return parseFloat(price).toFixed(2);
}

// Menge erhöhen
function increaseQuantity(itemId, itemName, itemPrice) {
    const existingItem = cart.find(item => item.menuItemId === itemId);

    if (existingItem) {
        existingItem.quantity += 1;
    } else {
        cart.push({
            menuItemId: itemId,
            name: itemName,
            quantity: 1,
            unitPrice: parseFloat(itemPrice)
        });
    }

    updateQuantityDisplay(itemId);
    renderCart();
}

// Menge verringern
function decreaseQuantity(itemId) {
    const existingItemIndex = cart.findIndex(item => item.menuItemId === itemId);

    if (existingItemIndex > -1) {
        if (cart[existingItemIndex].quantity > 1) {
            cart[existingItemIndex].quantity -= 1;
        } else {
            cart.splice(existingItemIndex, 1);
        }
    }

    updateQuantityDisplay(itemId);
    renderCart();
}

// Mengen-Anzeige aktualisieren
function updateQuantityDisplay(itemId) {
    const qtyDisplay = document.getElementById(`qty-${itemId}`);
    if (qtyDisplay) {
        const item = cart.find(i => i.menuItemId === itemId);
        qtyDisplay.textContent = item ? item.quantity : 0;
    }
}

// Item aus Warenkorb entfernen
function removeFromCart(itemId) {
    const index = cart.findIndex(item => item.menuItemId === itemId);
    if (index > -1) {
        cart.splice(index, 1);
        updateQuantityDisplay(itemId);
        renderCart();
    }
}

// Warenkorb rendern
function renderCart() {
    const cartItemsContainer = document.getElementById('cartItems');
    const submitBtn = document.getElementById('submitPreorder');

    if (cart.length === 0) {
        cartItemsContainer.innerHTML = '<p class="empty-cart-text">Noch keine Gerichte ausgewählt</p>';
        submitBtn.disabled = true;
    } else {
        cartItemsContainer.innerHTML = '';
        cart.forEach(item => {
            const cartItem = document.createElement('div');
            cartItem.className = 'cart-item';
            cartItem.innerHTML = `
                <div class="cart-item-info">
                    <span class="cart-item-name">${item.name}</span>
                    <span class="cart-item-qty">x${item.quantity}</span>
                </div>
                <div class="cart-item-price">
                    ${formatPrice(item.unitPrice * item.quantity)} €
                </div>
                <button class="cart-item-remove" onclick="removeFromCart(${item.menuItemId})">×</button>
            `;
            cartItemsContainer.appendChild(cartItem);
        });
        submitBtn.disabled = false;
    }

    // Gesamtsumme aktualisieren
    const total = calculateTotal();
    document.getElementById('cartTotal').textContent = formatPrice(total) + ' €';
}

// Gesamtpreis berechnen
function calculateTotal() {
    return cart.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0);
}

// Preorder absenden
async function submitPreorder() {
    if (cart.length === 0) {
        alert('Bitte fügen Sie mindestens ein Gericht hinzu.');
        return;
    }

    const specialRequests = document.getElementById('specialRequests').value;

    const preorderData = {
        reservationId: reservationId ? parseInt(reservationId) : null,
        restaurantId: restaurantId ? parseInt(restaurantId) : 1,
        tableNumber: null, // Wird später zugewiesen
        items: cart.map(item => ({
            menuItemId: item.menuItemId,
            name: item.name,
            quantity: item.quantity,
            unitPrice: item.unitPrice,
            specialInstructions: null
        })),
        specialRequests: specialRequests || null,
        deliveryTime: reservationDateTime || null
    };

    try {
        const response = await fetch(`${COOK_API}/orders/preorder`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(preorderData)
        });

        if (!response.ok) {
            throw new Error('Vorbestellung konnte nicht abgesendet werden');
        }

        const result = await response.json();

        alert(`Vorbestellung erfolgreich!\n\nBestellnummer: ${result.id}\nGesamtsumme: ${formatPrice(result.totalPrice)} €\n\nIhre Bestellung wird zur Reservierungszeit vorbereitet.`);

        // Zurück zur Startseite
        window.location.href = 'index.html';

    } catch (error) {
        console.error('Fehler beim Absenden der Vorbestellung:', error);
        alert('Fehler beim Absenden der Vorbestellung: ' + error.message);
    }
}

// Ohne Vorbestellung fortfahren
function skipPreorder() {
    alert('Reservierung bestätigt!\n\nSie können vor Ort bestellen.');
    window.location.href = 'index.html';
}

// Event Listeners
document.addEventListener('DOMContentLoaded', () => {
    getUrlParams();
    loadMenu();

    document.getElementById('submitPreorder').addEventListener('click', submitPreorder);
    document.getElementById('skipPreorder').addEventListener('click', skipPreorder);
});
