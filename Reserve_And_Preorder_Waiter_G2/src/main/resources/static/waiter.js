// waiter.js - JS-Logik für Kellnerübersicht
(() => {
    const API = {
        state: '/api/waiter/state',
        served: id => `/api/orders/${id}/served`,
        clearTable: id => `/api/tables/${id}/clear`,
        finishTable: id => `/api/tables/${id}/finish`,
        checkIn: id => `/api/reservations/${id}/checkin`,
        walkin: `/api/reservations/walkin`,
        createOrder: '/api/orders'
    };

    let DATA = { tables: [], orders: [] };
    let currentOrderTableId = null;
    let currentOrderItems = [];

    const $ = sel => document.querySelector(sel);

    // ============ MODAL FUNCTIONS ============

    window.openOrderModal = function(tableId, table) {
        currentOrderTableId = tableId;
        currentOrderItems = [];
        $('#orderModalTitle').textContent = `Bestellung für ${table?.name || 'Tisch ' + tableId}`;
        $('#orderItemsList').innerHTML = '<div class="no-items">Noch keine Artikel hinzugefügt</div>';
        $('#newItemName').value = '';
        $('#newItemQty').value = '1';
        $('#orderPrice').value = '';
        $('#orderModal').style.display = 'flex';
        $('#newItemName').focus();
    };

    window.closeOrderModal = function() {
        $('#orderModal').style.display = 'none';
        currentOrderTableId = null;
        currentOrderItems = [];
    };

    window.addOrderItem = function() {
        const name = $('#newItemName').value.trim();
        const qty = parseInt($('#newItemQty').value) || 1;

        if (!name) {
            $('#newItemName').focus();
            return;
        }

        currentOrderItems.push({ name, qty });
        renderOrderItems();
        $('#newItemName').value = '';
        $('#newItemQty').value = '1';
        $('#newItemName').focus();
    };

    window.removeOrderItem = function(index) {
        currentOrderItems.splice(index, 1);
        renderOrderItems();
    };

    function renderOrderItems() {
        const list = $('#orderItemsList');
        if (currentOrderItems.length === 0) {
            list.innerHTML = '<div class="no-items">Noch keine Artikel hinzugefügt</div>';
            return;
        }

        list.innerHTML = currentOrderItems.map((item, i) => `
            <div class="order-item-entry">
                <span class="item-name">${item.name}</span>
                <span class="item-qty">× ${item.qty}</span>
                <button class="btn-remove" onclick="removeOrderItem(${i})">✕</button>
            </div>
        `).join('');
    }

    window.submitOrder = async function() {
        if (currentOrderItems.length === 0) {
            alert('Bitte mindestens einen Artikel hinzufügen!');
            return;
        }

        const price = parseFloat($('#orderPrice').value);
        if (isNaN(price) || price <= 0) {
            alert('Bitte einen gültigen Preis eingeben!');
            $('#orderPrice').focus();
            return;
        }

        try {
            const response = await fetch(API.createOrder, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    tableId: currentOrderTableId,
                    items: currentOrderItems,
                    totalPrice: price
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
                    ${o.totalPrice ? `<div class="order-detail-price">€ ${Number(o.totalPrice).toFixed(2)}</div>` : ''}
                </div>
            `).join('');
        }

        $('#viewOrdersModal').style.display = 'flex';
    };

    window.closeViewOrdersModal = function() {
        $('#viewOrdersModal').style.display = 'none';
    };

    // ============ PAYMENT MODAL ============

    window.openPaymentModal = function(tableName) {
        $('#paymentTableInfo').textContent = tableName;
        $('#paymentModal').style.display = 'flex';
    };

    window.closePaymentModal = function() {
        $('#paymentModal').style.display = 'none';
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
                    // Only show "Abservieren" if there are READY orders AND there are no unserved orders
                    if (hasReadyOrder && orderCount === DATA.orders.filter(o => o.tableId === t.id && (o.status === 'BEREIT' || o.status === 'READY')).length) {
                        buttonsHtml += `<button class="btn-green" data-clear="${t.id}">Fertige Bestellung abholen</button>`;
                    }
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
                const guests = prompt(`Wie viele Gäste am Tisch ${table?.name || tableId}?`);
                if(guests && !isNaN(guests)) {
                    const res = await fetch(API.walkin, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ tableId: tableId, numberOfGuests: parseInt(guests) })
                    });
                    if(res.ok) await load();
                    else alert('Fehler beim Registrieren');
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
                
                if (confirm(`Bezahlung für ${tableName} bestätigen?\nTisch wird für Reinigung vorbereitet.`)) {
                    try {
                        // Mark table as ready for cleaning
                        const res = await fetch(API.clearTable(tableId), { method:'POST' });
                        if (res.ok) {
                            await load();
                            openPaymentModal(tableName);
                        } else {
                            alert('Fehler bei der Bezahlung');
                        }
                    } catch (err) {
                        console.error('Payment error:', err);
                        alert('Fehler bei der Bezahlung');
                    }
                }
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
                ${o.totalPrice ? `<div style="margin-top:8px;font-weight:600;">€ ${Number(o.totalPrice).toFixed(2)}</div>` : ''}
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
            if (document.activeElement === $('#newItemName') || document.activeElement === $('#newItemQty')) {
                e.preventDefault();
                addOrderItem();
            }
        }
        if (e.key === 'Escape') {
            closeOrderModal();
            closeViewOrdersModal();
            closePaymentModal();
            closeCleanedModal();
        }
    });

    // Init
    document.addEventListener('DOMContentLoaded', () => {
        $('#q').addEventListener('input', () => { renderTables(); renderOrders(); });
        $('#refreshBtn').addEventListener('click', load);
        load();
        setInterval(load, 5000);
    });
})();
