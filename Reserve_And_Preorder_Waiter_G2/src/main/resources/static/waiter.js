// waiter.js - JS-Logik für Kellnerübersicht
(() => {
    const API = {
        state: '/api/waiter/state',
        served: id => `/api/orders/${id}/served`,
        clearTable: id => `/api/tables/${id}/clear`,
        finishTable: id => `/api/tables/${id}/finish`,
        checkIn: id => `/api/reservations/${id}/checkin`,
        walkin: `/api/reservations/walkin`,
        createOrder: '/api/orders',
        payCash: '/api/payments/cash',
        payCard: '/api/payments/credit-card'
    };

    const OWNER_API = 'http://localhost:8083/api';

    let DATA = { tables: [], orders: [] };
    let currentOrderTableId = null;
    let currentOrderItems = [];
    let currentOrderTotal = 0;
    let currentRestaurantId = 1;
    let currentOrderReservationId = null;

    let currentWalkinTableId = null;

    let currentPaymentTableId = null;
    let currentPaymentReservationId = null;
    let currentPaymentTotal = 0;

    const MENU_CACHE = {};

    // QR Code Scanner (Modal)
    let html5QrCode = null;
    let isScanning = false;

    function openQrScannerModal() {
        $('#qrScannerModal').style.display = 'flex';
    }

    function closeQrScannerModal() {
        if (isScanning) {
            stopQRScanner();
        }
        $('#qrScannerModal').style.display = 'none';
        $('#modalScanResult').innerHTML = '';
    }

    function startQRScanner() {
        if (isScanning) return;

        const qrReaderDiv = $('#modalQrReader');
        qrReaderDiv.style.display = 'block';
        $('#modalStartScanBtn').style.display = 'none';
        $('#modalStopScanBtn').style.display = 'inline-block';

        html5QrCode = new Html5Qrcode("modalQrReader");

        const config = { fps: 10, qrbox: { width: 250, height: 250 } };

        html5QrCode.start(
            { facingMode: "environment" },
            config,
            (decodedText) => handleQRCodeScan(decodedText),
            (errorMessage) => { /* Ignore scan errors */ }
        ).then(() => {
            isScanning = true;
        }).catch((err) => {
            $('#modalScanResult').innerHTML =
                '<p style="color:red; background: #ffebee; padding: 10px; border-radius: 5px;">Kamera konnte nicht gestartet werden: ' + err + '</p>';
        });
    }

    function stopQRScanner() {
        if (!html5QrCode || !isScanning) return;

        html5QrCode.stop().then(() => {
            isScanning = false;
            $('#modalQrReader').style.display = 'none';
            $('#modalStartScanBtn').style.display = 'inline-block';
            $('#modalStopScanBtn').style.display = 'none';
        });
    }

    async function handleQRCodeScan(qrCodeData) {
        stopQRScanner();

        // Token aus URL extrahieren
        let token = null;
        if (qrCodeData.includes('?token=')) {
            token = qrCodeData.split('?token=')[1].split('&')[0];
        } else {
            $('#modalScanResult').innerHTML =
                '<p style="color:red; background: #ffebee; padding: 10px; border-radius: 5px;">Ungültiger QR-Code</p>';
            return;
        }

        // Check-In durchführen
        try {
            const response = await fetch('http://localhost:8083/api/qr/checkin', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token: token })
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.message || 'Check-In fehlgeschlagen');
            }

            $('#modalScanResult').innerHTML =
                `<p style="color:green; background: #e8f5e9; padding: 10px; border-radius: 5px;">
                    <strong>✓ Check-In erfolgreich!</strong><br>
                    Reservierung #${result.reservation.id}<br>
                    Tisch: ${result.tableNumber || 'Wird zugewiesen'}<br>
                    Gäste: ${result.reservation.numberOfGuests}
                </p>`;

            // Daten neu laden und Modal nach 2 Sekunden schließen
            load();
            setTimeout(() => {
                closeQrScannerModal();
            }, 2000);

        } catch (error) {
            $('#modalScanResult').innerHTML =
                '<p style="color:red; background: #ffebee; padding: 10px; border-radius: 5px;"><strong>Fehler:</strong> ' + error.message + '</p>';
        }
    }

    window.openQrScannerModal = openQrScannerModal;
    window.closeQrScannerModal = closeQrScannerModal;

    const $ = sel => document.querySelector(sel);

    // ============ MODAL FUNCTIONS ============

    window.openOrderModal = function(tableId, table) {
        currentOrderTableId = tableId;
        currentOrderItems = [];
        currentOrderTotal = 0;
        currentRestaurantId = table?.restaurantId || 1;
        currentOrderReservationId = table?.currentReservationId || null;
        $('#orderModalTitle').textContent = `Bestellung für ${table?.name || 'Tisch ' + tableId}`;
        $('#orderItemsList').innerHTML = '<div class="no-items">Noch keine Artikel hinzugefügt</div>';
        $('#menuQty').value = '1';
        $('#orderTotal').textContent = '€ 0.00';
        loadMenuForOrder(currentRestaurantId);
        $('#orderModal').style.display = 'flex';
        $('#menuSelect').focus();
    };

    window.closeOrderModal = function() {
        $('#orderModal').style.display = 'none';
        currentOrderTableId = null;
        currentOrderItems = [];
        currentOrderReservationId = null;
    };

    window.openWalkinModal = function(table) {
        currentWalkinTableId = table.id;
        $('#walkinTableInfo').textContent = `${table.name} – Plätze: ${table.capacity}`;
        $('#walkinHint').textContent = '';
        $('#walkinGuests').value = '';
        $('#walkinModal').style.display = 'flex';
        $('#walkinGuests').focus();
    };

    window.closeWalkinModal = function() {
        $('#walkinModal').style.display = 'none';
        currentWalkinTableId = null;
    };

    window.submitWalkin = async function() {
        if (!currentWalkinTableId) return;
        const table = DATA.tables.find(t => t.id === currentWalkinTableId);
        const guests = parseInt($('#walkinGuests').value);

        if (!guests || guests <= 0) {
            $('#walkinHint').textContent = 'Bitte eine gültige Gästeanzahl eingeben.';
            return;
        }

        if (table && table.capacity && guests > table.capacity) {
            $('#walkinHint').textContent = `Zu viele Gäste für diesen Tisch (max. ${table.capacity}).`;
            return;
        }

        try {
            const res = await fetch(API.walkin, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ tableId: currentWalkinTableId, numberOfGuests: guests })
            });
            if (res.ok) {
                closeWalkinModal();
                await load();
            } else {
                $('#walkinHint').textContent = 'Fehler beim Registrieren.';
            }
        } catch (e) {
            $('#walkinHint').textContent = 'Verbindungsfehler.';
        }
    };

    async function loadMenuForOrder(restaurantId) {
        const menuSelect = $('#menuSelect');
        menuSelect.innerHTML = '<option value="">Lade Speisekarte...</option>';

        if (MENU_CACHE[restaurantId]) {
            renderMenuSelect(MENU_CACHE[restaurantId]);
            return;
        }

        try {
            const res = await fetch(`${OWNER_API}/menu/${restaurantId}/available`);
            if (!res.ok) throw new Error('Speisekarte konnte nicht geladen werden');
            const menuItems = await res.json();
            if (menuItems && menuItems.length > 0) {
                MENU_CACHE[restaurantId] = menuItems;
                renderMenuSelect(menuItems);
            } else if (restaurantId !== 1) {
                const fallbackRes = await fetch(`${OWNER_API}/menu/1/available`);
                const fallbackItems = fallbackRes.ok ? await fallbackRes.json() : [];
                MENU_CACHE[1] = fallbackItems;
                renderMenuSelect(fallbackItems);
            } else {
                renderMenuSelect(menuItems);
            }
        } catch (e) {
            console.error('Fehler beim Laden der Speisekarte:', e);
            menuSelect.innerHTML = '<option value="">Fehler beim Laden</option>';
        }
    }

    function renderMenuSelect(menuItems) {
        const menuSelect = $('#menuSelect');
        if (!menuItems || menuItems.length === 0) {
            menuSelect.innerHTML = '<option value="">Keine Produkte verfügbar</option>';
            return;
        }

        const grouped = menuItems.reduce((acc, item) => {
            const category = item.category || 'Sonstiges';
            acc[category] = acc[category] || [];
            acc[category].push(item);
            return acc;
        }, {});

        const groupsHtml = Object.keys(grouped).map(category => {
            const opts = grouped[category]
                .map(item => `<option value="${item.id}">${item.name} – € ${Number(item.price).toFixed(2)}</option>`)
                .join('');
            return `<optgroup label="${category}">${opts}</optgroup>`;
        }).join('');

        menuSelect.innerHTML = '<option value="">Produkt wählen</option>' + groupsHtml;
    }

    window.addOrderItem = function() {
        const menuSelect = $('#menuSelect');
        const selectedId = menuSelect.value;
        const qty = parseInt($('#menuQty').value) || 1;

        if (!selectedId) {
            menuSelect.focus();
            return;
        }

        const item = (MENU_CACHE[currentRestaurantId] || []).find(m => String(m.id) === String(selectedId));
        if (!item) {
            alert('Bitte ein gültiges Menü-Produkt auswählen.');
            return;
        }

        const existing = currentOrderItems.find(i => i.menuItemId === item.id);
        if (existing) {
            existing.qty += qty;
        } else {
            currentOrderItems.push({
                menuItemId: item.id,
                name: item.name,
                qty,
                unitPrice: parseFloat(item.price)
            });
        }

        renderOrderItems();
        $('#menuQty').value = '1';
        menuSelect.focus();
    };

    window.removeOrderItem = function(index) {
        currentOrderItems.splice(index, 1);
        renderOrderItems();
    };

    function renderOrderItems() {
        const list = $('#orderItemsList');
        if (currentOrderItems.length === 0) {
            list.innerHTML = '<div class="no-items">Noch keine Artikel hinzugefügt</div>';
            currentOrderTotal = 0;
            $('#orderTotal').textContent = '€ 0.00';
            return;
        }

        list.innerHTML = currentOrderItems.map((item, i) => `
            <div class="order-item-entry">
                <span class="item-name">${item.name}</span>
                <span class="item-qty">× ${item.qty}</span>
                <span class="item-qty">€ ${(item.unitPrice * item.qty).toFixed(2)}</span>
                <button class="btn-remove" onclick="removeOrderItem(${i})">✕</button>
            </div>
        `).join('');

        currentOrderTotal = currentOrderItems.reduce((sum, item) => sum + (item.unitPrice * item.qty), 0);
        $('#orderTotal').textContent = `€ ${currentOrderTotal.toFixed(2)}`;
    }

    window.submitOrder = async function() {
        if (currentOrderItems.length === 0) {
            alert('Bitte mindestens einen Artikel hinzufügen!');
            return;
        }

        try {
            const response = await fetch(API.createOrder, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    tableId: currentOrderTableId,
                    reservationId: currentOrderReservationId,
                    items: currentOrderItems,
                    totalPrice: currentOrderTotal
                })
            });

            if (response.ok) {
                closeOrderModal();
                await load();
            } else {
                const error = await response.text();
                alert('Fehler: ' + error);
            }
        } catch (e) {
            alert('Verbindungsfehler: ' + e.message);
        }
    };

    // ============ VIEW ORDERS MODAL ============

    window.openViewOrdersModal = function(tableId) {
        const table = DATA.tables.find(t => t.id === tableId);
        const orders = DATA.orders.filter(o => o.tableId === tableId);

        $('#viewOrdersTitle').textContent = `Bestellungen - ${table?.name || 'Tisch ' + tableId}`;

        if (orders.length === 0) {
            $('#viewOrdersContent').innerHTML = '<div class="no-items">Keine Bestellungen für diesen Tisch</div>';
        } else {
            $('#viewOrdersContent').innerHTML = orders.map(o => `
                <div class="order-detail-card status-${o.status}">
                    <div class="order-detail-header">
                        <strong>#${o.id}</strong>
                        <span class="badge status-${o.status}">${o.status}</span>
                    </div>
                    <div class="order-detail-items">
                        ${(o.items||[]).map(i => `• ${i.name} × ${i.qty}`).join('<br>') || 'Keine Items'}
                    </div>
                    ${o.totalPrice != null ? `<div class="order-detail-price">€ ${Number(o.totalPrice).toFixed(2)}</div>` : ''}
                </div>
            `).join('');
        }

        $('#viewOrdersModal').style.display = 'flex';
    };

    window.closeViewOrdersModal = function() {
        $('#viewOrdersModal').style.display = 'none';
    };

    // ============ PAYMENT MODAL ============

    window.openPaymentModal = function(tableId, tableName, totalAmount, reservationId) {
        currentPaymentTableId = tableId;
        currentPaymentReservationId = reservationId;
        currentPaymentTotal = totalAmount;

        $('#paymentTableInfo').textContent = tableName;
        $('#paymentTotal').textContent = `€ ${totalAmount.toFixed(2)}`;
        $('#cashReceived').value = '';
        $('#changeAmount').textContent = '€ 0.00';

        const methods = document.querySelectorAll('input[name="paymentMethod"]');
        methods.forEach(m => { if (m.value === 'CARD') m.checked = true; });
        $('#cashSection').style.display = 'none';

        // Toggle cash section on method change
        methods.forEach(m => m.onchange = () => {
            const isCash = document.querySelector('input[name="paymentMethod"]:checked').value === 'CASH';
            $('#cashSection').style.display = isCash ? 'block' : 'none';
        });

        $('#cashReceived').oninput = () => {
            const received = parseFloat($('#cashReceived').value);
            if (!isNaN(received)) {
                const change = Math.max(0, received - currentPaymentTotal);
                $('#changeAmount').textContent = `€ ${change.toFixed(2)}`;
            } else {
                $('#changeAmount').textContent = '€ 0.00';
            }
        };

        $('#paymentModal').style.display = 'flex';
    };

    window.closePaymentModal = function() {
        $('#paymentModal').style.display = 'none';
        currentPaymentTableId = null;
        currentPaymentReservationId = null;
        currentPaymentTotal = 0;
    };

    window.confirmPayment = async function() {
        const method = document.querySelector('input[name="paymentMethod"]:checked').value;
        const total = currentPaymentTotal;

        if (!currentPaymentReservationId) {
            alert('Keine Reservierung für diesen Tisch gefunden.');
            return;
        }

        let change = 0;

        if (method === 'CASH') {
            const received = parseFloat($('#cashReceived').value);
            if (isNaN(received) || received <= 0) {
                alert('Bitte den erhaltenen Betrag eingeben.');
                $('#cashReceived').focus();
                return;
            }
            if (received < total) {
                alert('Der erhaltene Betrag ist zu niedrig.');
                $('#cashReceived').focus();
                return;
            }
            change = received - total;
        }

        try {
            const endpoint = method === 'CASH' ? API.payCash : API.payCard;
            const body = {
                reservationId: currentPaymentReservationId,
                amount: total,
                token: method === 'CARD' ? 'tok_waiter' : undefined
            };

            const res = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });

            if (!res.ok) {
                const errorText = await res.text();
                throw new Error(errorText || 'Zahlung fehlgeschlagen');
            }

            await fetch(API.clearTable(currentPaymentTableId), { method: 'POST' });
            closePaymentModal();
            await load();

            if (method === 'CASH') {
                alert(`Zahlung erfolgreich! Rückgeld: € ${change.toFixed(2)}`);
            } else {
                alert('Kartenzahlung erfolgreich!');
            }
        } catch (err) {
            console.error('Payment error:', err);
            alert('Fehler bei der Bezahlung: ' + err.message);
        }
    };

    // ============ CLEANED MODAL ============

    window.openCleanedModal = function(tableName) {
        $('#cleanedTableInfo').textContent = tableName;
        $('#cleanedModal').style.display = 'flex';
    };

    window.closeCleanedModal = function() {
        $('#cleanedModal').style.display = 'none';
    };

    // ============ RENDER FUNCTIONS ============

    function renderTables(){
        const q = $('#q').value.toLowerCase().trim();
        const wrap = $('#tables'); wrap.innerHTML = '';

        if (!DATA.tables || DATA.tables.length === 0) {
            wrap.innerHTML = '<div style="padding:20px;text-align:center;color:#999;">Keine Tische vorhanden.</div>';
            return;
        }

        DATA.tables
            .filter(t => !q || (t.name?.toLowerCase().includes(q) || String(t.id).includes(q)))
            .forEach(t => {
                // Debug log for RESERVIERT tables
                if (t.status === 'RESERVIERT') {
                    console.log(`Table ${t.name} (ID: ${t.id}) - Status: ${t.status}, currentReservationId: ${t.currentReservationId}`);
                }
                
                // Check if there's a READY order for this table
                const hasReadyOrder = DATA.orders.some(o =>
                    o.tableId === t.id && (o.status === 'BEREIT' || o.status === 'READY')
                );
                const orderCount = DATA.orders.filter(o =>
                    o.tableId === t.id && o.status !== 'SERVIERT' && o.status !== 'SERVED'
                ).length;

                const div = document.createElement('div');
                div.className = 'table-card';

                let buttonsHtml = '';

                // Always show "Bestellungen" button
                buttonsHtml += `<button data-view="${t.id}">Bestellungen${orderCount > 0 ? ` (${orderCount})` : ''}</button>`;

                if (t.status === 'LEER') {
                    buttonsHtml += `<button class="btn-gray" data-walkin="${t.id}">+ Walk-in</button>`;
                } else if (t.status === 'RESERVIERT') {
                    if (t.currentReservationId) {
                        buttonsHtml += `<button class="btn-blue" data-checkin="${t.currentReservationId}">✓ Angekommen</button>`;
                    } else {
                        buttonsHtml += `<button class="btn-gray" disabled title="Keine Reservierung zugewiesen">⚠️ Keine Reservierung</button>`;
                    }
                } else if (t.status === 'BELEGT') {
                    buttonsHtml += `<button class="btn-orange" data-order="${t.id}">+ Bestellung aufnehmen</button>`;
                    // Only show "Bezahlen" if all orders are served (orderCount === 0)
                    if (orderCount === 0) {
                        buttonsHtml += `<button class="btn-blue" data-pay="${t.id}">Bezahlen</button>`;
                    }
                } else if (t.status === 'ABSERVIEREN') {
                    // After payment, only show "Tisch abservieren" to mark as clean
                    buttonsHtml += `<button class="btn-green" data-finish="${t.id}">Tisch abservieren</button>`;
                }

                div.innerHTML = `
                    <div class="row">
                        <strong>${t.name ?? ('Tisch ' + t.id)}</strong>
                        <span class="badge status-${t.status}">${t.status}</span>
                    </div>
                    <div>Sitze: ${t.capacity ?? '-'}</div>
                    <div class="btn-row">${buttonsHtml}</div>
                `;
                wrap.appendChild(div);
            });

        wrap.onclick = async (e) => {
            const b = e.target.closest('button');
            if(!b) return;

            if(b.dataset.view) {
                openViewOrdersModal(Number(b.dataset.view));
            }
            if(b.dataset.walkin) {
                const tableId = Number(b.dataset.walkin);
                const table = DATA.tables.find(t => t.id === tableId);
                if (table) {
                    openWalkinModal(table);
                }
            }
            if(b.dataset.checkin) {
                const resId = Number(b.dataset.checkin);
                try {
                    const res = await fetch(API.checkIn(resId), { method:'POST' });
                    if(res.ok) {
                        await load();
                    } else {
                        const errorText = await res.text();
                        console.error('Check-in error:', errorText);
                        alert('Fehler beim Einchecken:\n' + (errorText || 'Unbekannter Fehler. Bitte prüfen Sie ob ein Tisch zugewiesen wurde.'));
                    }
                } catch (err) {
                    console.error('Check-in error:', err);
                    alert('Verbindungsfehler beim Einchecken: ' + err.message);
                }
            }
            if(b.dataset.clear) {
                await fetch(API.clearTable(b.dataset.clear), { method:'POST' });
                await load();
            }
            if(b.dataset.finish) {
                const tableId = Number(b.dataset.finish);
                const table = DATA.tables.find(t => t.id === tableId);
                const tableName = table?.name || 'Tisch ' + tableId;
                
                if (confirm(`Tisch ${tableName} abservieren und freigeben?`)) {
                    try {
                        const res = await fetch(API.finishTable(tableId), { method:'POST' });
                        if (res.ok) {
                            openCleanedModal(tableName);
                        } else {
                            alert('Fehler beim Abservieren');
                        }
                    } catch (err) {
                        console.error('Finish table error:', err);
                        alert('Fehler beim Abservieren');
                    }
                    await load();
                }
            }
            if(b.dataset.order) {
                const tableId = Number(b.dataset.order);
                const table = DATA.tables.find(t => t.id === tableId);
                openOrderModal(tableId, table);
            }
            if(b.dataset.pay) {
                const tableId = Number(b.dataset.pay);
                const table = DATA.tables.find(t => t.id === tableId);
                const tableName = table?.name || 'Tisch ' + tableId;
                const reservationId = table?.currentReservationId || null;
                
                // Check if there are any unserved orders
                const unservedOrders = DATA.orders.filter(o => 
                    o.tableId === tableId && 
                    o.status !== 'SERVIERT' && 
                    o.status !== 'SERVED' && 
                    o.status !== 'CANCELLED'
                );
                
                if (unservedOrders.length > 0) {
                    alert(`Es gibt noch ${unservedOrders.length} nicht servierte Bestellung(en)!\nBitte erst alle Bestellungen servieren.`);
                    return;
                }
                
                if (!reservationId) {
                    alert('Keine Reservierung für diesen Tisch gefunden.');
                    return;
                }

                const tableOrders = DATA.orders.filter(o => {
                    if (reservationId) {
                        return o.reservationId === reservationId && o.status !== 'CANCELLED';
                    }
                    return o.tableId === tableId && o.status !== 'CANCELLED';
                });
                console.log('Pay orders for table', tableId, tableOrders);
                const total = tableOrders.reduce((sum, o) => {
                    const val = Number(o.totalPrice || 0);
                    if (!isNaN(val) && val > 0) {
                        return sum + val;
                    }
                    // Fallback: sum items with unitPrice
                    const itemsTotal = (o.items || []).reduce((s, i) => {
                        const unit = Number(i.unitPrice || 0);
                        const qty = Number(i.qty || 0);
                        return s + (isNaN(unit) ? 0 : unit * qty);
                    }, 0);
                    return sum + itemsTotal;
                }, 0);
                console.log('Computed total', total);

                if (total <= 0) {
                    const manual = prompt('Kein gültiger Gesamtbetrag gefunden. Betrag manuell eingeben (€):');
                    const manualVal = parseFloat(manual);
                    if (!manual || isNaN(manualVal) || manualVal <= 0) {
                        alert('Kein gültiger Betrag eingegeben.');
                        return;
                    }
                    openPaymentModal(tableId, tableName, manualVal, reservationId);
                    return;
                }

                openPaymentModal(tableId, tableName, total, reservationId);
            }
        };
    }

    function renderOrders(){
        const q = $('#q').value.toLowerCase().trim();
        const wrap = $('#orders'); wrap.innerHTML = '';

        const openOrders = DATA.orders
            .filter(o => o.status !== 'SERVIERT' && o.status !== 'SERVED' && o.status !== 'CANCELLED')
            .filter(o => {
                if(!q) return true;
                const t = DATA.tables.find(t => t.id === o.tableId);
                const tableName = (t?.name || ('Tisch '+o.tableId)).toLowerCase();
                const items = (o.items||[]).map(i=>`${i.name} x${i.qty}`).join(' ').toLowerCase();
                return String(o.id).includes(q) || String(o.tableId).includes(q) || tableName.includes(q) || items.includes(q);
            });

        if (openOrders.length === 0) {
            wrap.innerHTML = '<div style="padding:20px;text-align:center;color:#999;">Keine offenen Bestellungen</div>';
            return;
        }

        openOrders.forEach(o => {
            const t = DATA.tables.find(t => t.id === o.tableId);
            const tableName = t?.name || ('Tisch ' + o.tableId);
            const isReady = o.status === 'BEREIT' || o.status === 'READY';

            const div = document.createElement('div');
            div.className = 'order';
            div.dataset.table = o.tableId;
            div.innerHTML = `
                <div class="row">
                    <div><strong>#${o.id}</strong> – ${tableName}</div>
                    <span class="badge status-${o.status}">${o.status}</span>
                </div>
                <div class="items">${(o.items||[]).map(i=>`• ${i.name} × ${i.qty}`).join('<br>') || 'Keine Items'}</div>
                ${o.totalPrice != null ? `<div style="margin-top:8px;font-weight:600;">€ ${Number(o.totalPrice).toFixed(2)}</div>` : ''}
                <div style="text-align:right;margin-top:10px;">
                    ${isReady
                        ? `<button class="btn-green" data-served="${o.id}">✓ Serviert</button>`
                        : `<button disabled title="Warte auf Koch">⏳ In Zubereitung</button>`
                    }
                </div>
            `;
            wrap.appendChild(div);
        });

        wrap.onclick = async (e) => {
            const b = e.target.closest('button[data-served]');
            if(!b) return;
            await fetch(API.served(b.dataset.served), { method:'POST' });
            await load();
        };
    }

    async function load(){
        try {
            const res = await fetch(API.state, { headers: { 'Accept':'application/json' } });

            if(!res.ok){
                $('#tables').innerHTML = '<div style="padding:20px;color:red;">Fehler beim Laden: ' + res.status + '</div>';
                return;
            }

            DATA = await res.json();
            renderTables();
            renderOrders();
        } catch (err) {
            console.error('Fehler:', err);
            $('#tables').innerHTML = '<div style="padding:20px;color:red;">Verbindungsfehler</div>';
        }
    }

    // Handle Enter key in order modal
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && $('#orderModal').style.display === 'flex') {
            if (document.activeElement === $('#menuSelect') || document.activeElement === $('#menuQty')) {
                e.preventDefault();
                addOrderItem();
            }
        }
        if (e.key === 'Escape') {
            closeOrderModal();
            closeViewOrdersModal();
            closePaymentModal();
            closeCleanedModal();
            closeWalkinModal();
            closeQrScannerModal();
        }
    });

    // Init
    document.addEventListener('DOMContentLoaded', () => {
        $('#q').addEventListener('input', () => { renderTables(); renderOrders(); });
        $('#refreshBtn').addEventListener('click', load);

        // QR-Scanner Modal Event Listeners
        $('#openQrScannerBtn')?.addEventListener('click', openQrScannerModal);
        $('#modalStartScanBtn')?.addEventListener('click', startQRScanner);
        $('#modalStopScanBtn')?.addEventListener('click', stopQRScanner);

        load();
        setInterval(load, 5000);
    });
})();
