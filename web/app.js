// Smart Parking Lot Management System - Real-time Full Stack Web App JS

const STATE = {
    slots: [],
    tickets: [],
    totalRevenue: 0.0,
    currentFilter: 'all',
    selectedTicketForCheckout: null,
    isApiConnected: false
};

const BASE_HOURLY_RATE = 10.0;
const MULTIPLIERS = { BIKE: 0.5, CAR: 1.0, TRUCK: 2.0 };
const ICONS = { BIKE: '🏍️', CAR: '🚗', TRUCK: '🚛' };

// DOM Elements
const slotsGrid = document.getElementById('slots-grid');
const activeTicketsList = document.getElementById('active-tickets-list');
const historyTbody = document.getElementById('history-tbody');
const parkForm = document.getElementById('park-form');
const licenseInput = document.getElementById('license-input');

const statTotal = document.getElementById('stat-total');
const statOccupied = document.getElementById('stat-occupied');
const statVacant = document.getElementById('stat-vacant');
const statRevenue = document.getElementById('stat-revenue');
const occupancyBar = document.getElementById('occupancy-bar');

const checkoutModal = document.getElementById('checkout-modal');
const modalClose = document.getElementById('modal-close');
const simHoursInput = document.getElementById('sim-hours');
const simHoursVal = document.getElementById('sim-hours-val');
const btnConfirmCheckout = document.getElementById('btn-confirm-checkout');
const btnResetLot = document.getElementById('btn-reset-lot');
const btnQuickPark = document.getElementById('btn-quick-park');

// Fetch Live Status from Java Server
async function fetchStatus() {
    try {
        const res = await fetch('/api/status');
        if (res.ok) {
            const data = await res.json();
            STATE.slots = data.slots || [];
            STATE.totalRevenue = data.totalRevenue || 0.0;
            STATE.isApiConnected = true;

            const histRes = await fetch('/api/history');
            if (histRes.ok) {
                STATE.tickets = await histRes.json();
            }
            render();
            return;
        }
    } catch (e) {
        console.warn("Backend REST API offline. Using interactive local simulation mode.");
    }
    
    // Fallback to local demo mode if opened directly without server
    if (STATE.slots.length === 0) {
        initLocalDemoData();
    }
}

function initLocalDemoData() {
    STATE.slots = [
        { id: 'B-01', type: 'BIKE', occupied: false, vehicle: null },
        { id: 'B-02', type: 'BIKE', occupied: false, vehicle: null },
        { id: 'B-03', type: 'BIKE', occupied: false, vehicle: null },
        { id: 'C-01', type: 'COMPACT', occupied: false, vehicle: null },
        { id: 'C-02', type: 'COMPACT', occupied: false, vehicle: null },
        { id: 'C-03', type: 'COMPACT', occupied: false, vehicle: null },
        { id: 'C-04', type: 'COMPACT', occupied: false, vehicle: null },
        { id: 'L-01', type: 'LARGE', occupied: false, vehicle: null },
        { id: 'L-02', type: 'LARGE', occupied: false, vehicle: null }
    ];
    render();
}

async function parkVehicle(licensePlate, vehicleType) {
    if (STATE.isApiConnected) {
        try {
            const res = await fetch('/api/park', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ licensePlate, vehicleType })
            });
            const data = await res.json();
            if (!res.ok) {
                alert(data.error || 'Failed to park vehicle');
                return false;
            }
            await fetchStatus();
            return true;
        } catch (e) {
            alert('API Communication Error: ' + e.message);
            return false;
        }
    }

    // Local Fallback
    return parkVehicleLocal(licensePlate, vehicleType);
}

function parkVehicleLocal(licensePlate, vehicleType) {
    licensePlate = licensePlate.trim().toUpperCase();
    const active = STATE.tickets.find(t => t.licensePlate === licensePlate && !t.paid);
    if (active) {
        alert(`Vehicle ${licensePlate} is already parked in slot ${active.slotId}!`);
        return false;
    }

    let available = STATE.slots.filter(s => !s.occupied && isCompatible(s.type, vehicleType));
    if (available.length === 0) {
        alert(`Parking Lot Full! No slot available for ${vehicleType}.`);
        return false;
    }

    let slot = available.find(s => getIdealSlot(vehicleType) === s.type) || available[0];
    slot.occupied = true;
    slot.vehicle = { licensePlate, vehicleType };

    const ticket = {
        ticketId: 'TKT-' + Math.floor(100000 + Math.random() * 900000),
        licensePlate,
        vehicleType,
        slotId: slot.id,
        entryTime: new Date().toISOString(),
        exitTime: null,
        fee: 0,
        paid: false
    };

    STATE.tickets.unshift(ticket);
    render();
    return true;
}

function isCompatible(slotType, vehicleType) {
    if (vehicleType === 'BIKE') return true;
    if (vehicleType === 'CAR') return slotType === 'COMPACT' || slotType === 'LARGE';
    if (vehicleType === 'TRUCK') return slotType === 'LARGE';
    return false;
}

function getIdealSlot(vehicleType) {
    if (vehicleType === 'BIKE') return 'BIKE';
    if (vehicleType === 'CAR') return 'COMPACT';
    return 'LARGE';
}

async function checkoutVehicle(ticketId, hours) {
    if (STATE.isApiConnected) {
        try {
            const res = await fetch('/api/unpark', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ticketId, hours: hours.toString() })
            });
            const data = await res.json();
            if (!res.ok) {
                alert(data.error || 'Failed to checkout vehicle');
                return false;
            }
            await fetchStatus();
            return true;
        } catch (e) {
            alert('API Communication Error: ' + e.message);
            return false;
        }
    }

    // Local Checkout Fallback
    const ticket = STATE.selectedTicketForCheckout;
    if (!ticket) return false;

    const fee = calculateFee(ticket.vehicleType, hours);
    ticket.paid = true;
    ticket.fee = fee;
    ticket.exitTime = new Date(new Date(ticket.entryTime).getTime() + hours * 3600000).toISOString();

    const slot = STATE.slots.find(s => s.id === ticket.slotId);
    if (slot) {
        slot.occupied = false;
        slot.vehicle = null;
    }

    STATE.totalRevenue += fee;
    render();
    return true;
}

function calculateFee(vehicleType, hours) {
    const mult = MULTIPLIERS[vehicleType] || 1.0;
    return Math.max(1, hours) * BASE_HOURLY_RATE * mult;
}

// Render Functions
function render() {
    renderStats();
    renderSlots();
    renderActiveTickets();
    renderHistory();
}

function renderStats() {
    const total = STATE.slots.length;
    const occupied = STATE.slots.filter(s => s.occupied).length;
    const vacant = total - occupied;
    const pct = total > 0 ? Math.round((occupied / total) * 100) : 0;

    statTotal.textContent = total;
    statOccupied.textContent = occupied;
    statVacant.textContent = vacant;
    statRevenue.textContent = `$${STATE.totalRevenue.toFixed(2)}`;
    occupancyBar.style.width = `${pct}%`;
}

function renderSlots() {
    slotsGrid.innerHTML = '';
    const filtered = STATE.slots.filter(s => {
        if (STATE.currentFilter === 'all') return true;
        if (STATE.currentFilter === 'bike') return s.type === 'BIKE';
        if (STATE.currentFilter === 'compact') return s.type === 'COMPACT';
        if (STATE.currentFilter === 'large') return s.type === 'LARGE';
        return true;
    });

    filtered.forEach(slot => {
        const card = document.createElement('div');
        card.className = `slot-card ${slot.occupied ? 'occupied' : 'vacant'}`;

        const icon = slot.occupied && slot.vehicle ? ICONS[slot.vehicle.vehicleType] : '🅿️';
        const licenseHtml = slot.occupied && slot.vehicle ? 
            `<div class="slot-license">${slot.vehicle.licensePlate}</div>
             <button class="btn-unpark-mini" onclick="openCheckoutModalBySlot('${slot.id}')">Checkout</button>` : 
            `<span class="slot-status-label">Vacant</span>`;

        card.innerHTML = `
            <span class="slot-id-badge">${slot.id} (${slot.type.substring(0, 1)})</span>
            <div class="slot-vehicle-icon">${icon}</div>
            ${licenseHtml}
        `;
        slotsGrid.appendChild(card);
    });
}

function renderActiveTickets() {
    activeTicketsList.innerHTML = '';
    const active = STATE.tickets.filter(t => !t.paid);

    if (active.length === 0) {
        activeTicketsList.innerHTML = `<div style="text-align: center; color: var(--text-secondary); font-size: 13px; padding: 12px;">No active parking sessions</div>`;
        return;
    }

    active.forEach(t => {
        const item = document.createElement('div');
        item.className = 'active-ticket-item';
        item.innerHTML = `
            <div>
                <div class="tkt-license">${ICONS[t.vehicleType] || '🚗'} ${t.licensePlate}</div>
                <div class="tkt-sub">${t.slotId} • ${t.ticketId}</div>
            </div>
            <button class="btn btn-secondary" style="font-size: 11px; padding: 4px 8px;" onclick="openCheckoutModal('${t.ticketId}')">Checkout</button>
        `;
        activeTicketsList.appendChild(item);
    });
}

function renderHistory() {
    historyTbody.innerHTML = '';
    if (STATE.tickets.length === 0) {
        historyTbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color: var(--text-secondary);">No history recorded yet</td></tr>`;
        return;
    }

    STATE.tickets.forEach(t => {
        const tr = document.createElement('tr');
        const entryStr = t.entryTime ? new Date(t.entryTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '-';
        const exitStr = t.exitTime ? new Date(t.exitTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '-';
        const statusBadge = t.paid ? `<span class="badge badge-paid">PAID</span>` : `<span class="badge badge-active">PARKED</span>`;

        tr.innerHTML = `
            <td><strong>${t.ticketId}</strong></td>
            <td>${t.licensePlate}</td>
            <td>${ICONS[t.vehicleType] || ''} ${t.vehicleType}</td>
            <td><span class="slot-id-badge">${t.slotId}</span></td>
            <td>${entryStr}</td>
            <td>${exitStr}</td>
            <td><strong>$${(t.fee || 0).toFixed(2)}</strong></td>
            <td>${statusBadge}</td>
        `;
        historyTbody.appendChild(tr);
    });
}

// Modal Handlers
window.openCheckoutModalBySlot = function(slotId) {
    const active = STATE.tickets.find(t => t.slotId === slotId && !t.paid);
    if (active) openCheckoutModal(active.ticketId);
};

window.openCheckoutModal = function(ticketId) {
    const ticket = STATE.tickets.find(t => t.ticketId === ticketId);
    if (!ticket) return;

    STATE.selectedTicketForCheckout = ticket;
    simHoursInput.value = 1;
    simHoursVal.textContent = '1 hr';

    updateReceiptView();
    checkoutModal.classList.remove('hidden');
};

function updateReceiptView() {
    const ticket = STATE.selectedTicketForCheckout;
    if (!ticket) return;

    const hours = parseInt(simHoursInput.value, 10);
    const fee = calculateFee(ticket.vehicleType, hours);

    document.getElementById('rec-ticket-id').textContent = ticket.ticketId;
    document.getElementById('rec-license').textContent = ticket.licensePlate;
    document.getElementById('rec-type').textContent = `${ICONS[ticket.vehicleType] || ''} ${ticket.vehicleType}`;
    document.getElementById('rec-slot').textContent = ticket.slotId;
    document.getElementById('rec-entry').textContent = new Date(ticket.entryTime).toLocaleTimeString();

    const exitSimulated = new Date(new Date(ticket.entryTime).getTime() + hours * 3600000);
    document.getElementById('rec-exit').textContent = exitSimulated.toLocaleTimeString();
    document.getElementById('rec-duration').textContent = `${hours} hour(s)`;
    document.getElementById('rec-fee').textContent = `$${fee.toFixed(2)}`;
}

simHoursInput.addEventListener('input', (e) => {
    simHoursVal.textContent = `${e.target.value} hr${e.target.value > 1 ? 's' : ''}`;
    updateReceiptView();
});

modalClose.addEventListener('click', () => {
    checkoutModal.classList.add('hidden');
});

btnConfirmCheckout.addEventListener('click', async () => {
    const ticket = STATE.selectedTicketForCheckout;
    if (!ticket) return;

    const hours = parseInt(simHoursInput.value, 10);
    await checkoutVehicle(ticket.ticketId, hours);

    checkoutModal.classList.add('hidden');
});

// Event Listeners
parkForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const license = licenseInput.value.trim();
    const type = document.querySelector('input[name="vehicle-type"]:checked').value;

    if (await parkVehicle(license, type)) {
        licenseInput.value = '';
    }
});

document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        e.target.classList.add('active');
        STATE.currentFilter = e.target.dataset.zone;
        renderSlots();
    });
});

btnResetLot.addEventListener('click', async () => {
    if (confirm('Reset parking lot state in database?')) {
        if (STATE.isApiConnected) {
            await fetch('/api/reset', { method: 'POST' });
            await fetchStatus();
        } else {
            initLocalDemoData();
        }
    }
});

btnQuickPark.addEventListener('click', () => {
    licenseInput.focus();
});

// Initialize Status Fetch
fetchStatus();
