// waiter.js - JS-Logik für Kellnerübersicht (im Ordner waiter/)
(() => {
    const API = {
        state: '/api/waiter/state',
        served: id => `/api/orders/${id}/served`,
        clearTable: id => `/api/tables/${id}/clear`,
        finishTable: id => `/api/tables/${id}/finish`
    };

    let DATA = { tables: [], orders: [] };
    const $ = sel => document.querySelector(sel);

    function renderTables(){
        const q = $('#q').value.toLowerCase().trim();
        const wrap = $('#tables'); wrap.innerHTML = '';

        DATA.tables
            .filter(t => !q || (t.name?.toLowerCase().includes(q) || String(t.id).includes(q)))
            .forEach(t => {
                // Gibt es eine 'BEREIT' Bestellung für diesen Tisch?
                const hasReady = DATA.orders.some(o => o.tableId === t.id && o.status === 'BEREIT');
                const div = document.createElement('div');
                div.className = 'table-card';
                div.innerHTML = `
          <div class="row">
            <strong>${t.name ?? ('Tisch ' + t.id)}</strong>
            <span class="badge status-${t.status}">${t.status}</span>
          </div>
          <div>Sitze: ${t.seats ?? '-'}</div>
          <div class="row" style="gap:8px; flex-wrap:wrap;">
            <button data-jump="${t.id}">Bestellungen</button>
            ${t.status==='ESSEN'
                    ? `<button class="btn-green" data-clear="${t.id}">Abservieren</button>`
                    : t.status==='ABSERVIEREN'
                        // nur hier darf "Tisch fertig" erscheinen
                        ? `<button class="btn-green" data-finish="${t.id}">Tisch fertig</button>`
                        : ``}
          </div>
        `;
                wrap.appendChild(div);
            });

        wrap.onclick = async (e)=>{
            const b = e.target.closest('button');
            if(!b) return;
            if(b.dataset.jump){
                const tableId = Number(b.dataset.jump);
                const orders = DATA.orders.filter(o => o.tableId === tableId && o.status !== 'SERVIERT');
                if(orders.length === 0) {
                    alert("Keine offenen Bestellungen für diesen Tisch.");
                } else {
                    alert(orders.map(o =>
                        `#${o.id}: ${o.items.map(i=>`${i.name} x${i.qty}`).join(', ')}`
                    ).join('\n'));
                }
            }
            if(b.dataset.clear){
                await fetch(API.clearTable(b.dataset.clear), { method:'POST' });
                await load();
            }
            if(b.dataset.finish){
                const res = await fetch(API.finishTable(b.dataset.finish), { method:'POST' });
                if (res.status === 409) {
                    alert('Tisch kann nicht als fertig markiert werden — es gibt noch fertige Bestellungen (BEREIT) oder falschen Status.');
                }
                await load();
            }
        };
    }

    function renderOrders(){
        const q = $('#q').value.toLowerCase().trim();
        const wrap = $('#orders'); wrap.innerHTML = '';

        DATA.orders
            .filter(o => o.status !== 'SERVIERT') // nur offene zeigen
            .filter(o => {
                if(!q) return true;
                const t = DATA.tables.find(t => t.id === o.tableId);
                const tableName = (t?.name || ('Tisch '+o.tableId)).toLowerCase();
                const items = (o.items||[]).map(i=>`${i.name} x${i.qty}`).join(' ').toLowerCase();
                return String(o.id).includes(q) || String(o.tableId).includes(q) || tableName.includes(q) || items.includes(q);
            })
            .forEach(o => {
                const t = DATA.tables.find(t => t.id === o.tableId);
                const tableName = t?.name || ('Tisch ' + o.tableId);
                const div = document.createElement('div');
                div.className = 'order';
                div.dataset.table = o.tableId;
                div.innerHTML = `
          <div class="row">
            <div><strong>#${o.id}</strong> – ${tableName}</div>
            <span class="badge status-${o.status}">${o.status}</span>
          </div>
          <div class="items">${(o.items||[]).map(i=>`• ${i.name} × ${i.qty}`).join('<br>')}</div>
          <div style="text-align:right;margin-top:10px;">
            <button data-served="${o.id}">Serviert</button>
          </div>
        `;
                wrap.appendChild(div);
            });

        wrap.onclick = async (e)=>{
            const b = e.target.closest('button[data-served]');
            if(!b) return;
            await fetch(API.served(b.dataset.served), { method:'POST' });
            await load();
        };
    }

    async function load(){
        try {
            const res = await fetch(API.state, { headers: { 'Accept':'application/json' } });
            if(!res.ok){ console.error('API', res.status); return; }
            DATA = await res.json();
            renderTables(); renderOrders();
        } catch (err) {
            console.error('Fehler beim Laden des API-Status:', err);
        }
    }

    // Init nach DOM ready
    document.addEventListener('DOMContentLoaded', () => {
        $('#q').addEventListener('input', ()=>{ renderTables(); renderOrders(); });
        $('#refreshBtn').addEventListener('click', load);
        load();
        setInterval(load, 10000);
    });
})();
