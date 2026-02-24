/* SwiftTrack – Frontend Application Logic */

"use strict";

/* MOCK DATA  –  Simulated JSON payloads */
const MockData = (() => {

    /** All orders the client can see */
    const orders = [
        { id: "SL-1001", customer: "Nimal Perera",    address: "42 Galle Rd, Colombo 03",    contact: "077-1234567", priority: "Normal", status: "Delivered",   driver: "Kasun Silva",    updated: "2026-02-24 08:22" },
        { id: "SL-1002", customer: "Amaya Fernando",  address: "15 Kandy Rd, Peradeniya",    contact: "071-9876543", priority: "High",   status: "In Transit",  driver: "Ruwan Dias",     updated: "2026-02-24 09:10" },
        { id: "SL-1003", customer: "Saman Jayawardena", address: "8 Beach Rd, Galle",         contact: "076-5551234", priority: "Normal", status: "Processing",  driver: "Pending",        updated: "2026-02-24 09:45" },
        { id: "SL-1004", customer: "Dilini Rathnayake", address: "99 Temple Rd, Kandy",       contact: "078-4443322", priority: "High",   status: "Pending",     driver: "Unassigned",     updated: "2026-02-24 10:00" },
        { id: "SL-1005", customer: "Chamara Bandara",   address: "23 Lake Dr, Nuwara Eliya",  contact: "070-6667788", priority: "Normal", status: "Failed",      driver: "Kasun Silva",    updated: "2026-02-24 07:30" },
        { id: "SL-1006", customer: "Tharushi de Silva",  address: "5 Main St, Matara",         contact: "075-1112233", priority: "Normal", status: "In Transit",  driver: "Ruwan Dias",     updated: "2026-02-24 10:15" },
    ];

    /** Driver manifest (daily delivery list for Kasun Silva) */
    const manifest = [
        { id: "SL-1001", address: "42 Galle Rd, Colombo 03", customer: "Nimal Perera", contact: "077-1234567", priority: "Normal", status: "Delivered", eta: "08:30", notes: "" },
        { id: "SL-1002", address: "15 Kandy Rd, Peradeniya",  customer: "Amaya Fernando", contact: "071-9876543", priority: "High",   status: "In Transit", eta: "10:00", notes: "Fragile – handle with care" },
        { id: "SL-1005", address: "23 Lake Dr, Nuwara Eliya",  customer: "Chamara Bandara", contact: "070-6667788", priority: "Normal", status: "Failed", eta: "12:00", notes: "Customer not available" },
        { id: "SL-1007", address: "78 Hill St, Badulla",        customer: "Iresha Kumari", contact: "072-8889900", priority: "Normal", status: "Pending", eta: "14:30", notes: "" },
        { id: "SL-1008", address: "3 Fort Rd, Jaffna",          customer: "Pradeep Kumar", contact: "077-3334455", priority: "High",   status: "Pending", eta: "16:00", notes: "Call before delivery" },
    ];

    /** Optimised route steps (from ROS) */
    const routeSteps = [
        { seq: 1, address: "42 Galle Rd, Colombo 03",     orderId: "SL-1001", status: "completed" },
        { seq: 2, address: "15 Kandy Rd, Peradeniya",     orderId: "SL-1002", status: "current"   },
        { seq: 3, address: "23 Lake Dr, Nuwara Eliya",    orderId: "SL-1005", status: "upcoming"  },
        { seq: 4, address: "78 Hill St, Badulla",          orderId: "SL-1007", status: "upcoming"  },
        { seq: 5, address: "3 Fort Rd, Jaffna",            orderId: "SL-1008", status: "upcoming"  },
    ];

    /** Tracking timeline for a single order */
    const trackingTimeline = [
        { step: "Order Created",               time: "2026-02-24 08:00", done: true  },
        { step: "Sent to Warehouse (WMS/TCP)", time: "2026-02-24 08:05", done: true  },
        { step: "Warehouse Confirmed (CMS/SOAP)", time: "2026-02-24 08:12", done: true  },
        { step: "Route Optimised (ROS/REST)",  time: "2026-02-24 08:18", done: true  },
        { step: "Driver Assigned",             time: "2026-02-24 08:20", done: true  },
        { step: "Out for Delivery",            time: "2026-02-24 08:22", done: true  },
        { step: "In Transit",                  time: "2026-02-24 09:10", done: false, current: true },
        { step: "Delivered",                   time: "",                 done: false  },
    ];

    /** Failure reasons dropdown */
    const failReasons = [
        "Customer not available",
        "Wrong address",
        "Package damaged",
        "Customer refused delivery",
        "Access restricted",
        "Other",
    ];

    /** Simulated real-time events (for demo button) */
    const realtimeEvents = [
        { orderId: "SL-1003", newStatus: "In Transit",  driver: "Nuwan Bandara", message: "Order SL-1003 is now In Transit!" },
        { orderId: "SL-1004", newStatus: "Processing",  driver: "Kasun Silva",   message: "Order SL-1004 picked up by warehouse." },
        { orderId: "SL-1002", newStatus: "Delivered",    driver: "Ruwan Dias",    message: "Order SL-1002 has been delivered!" },
        { orderId: "SL-1006", newStatus: "Delivered",    driver: "Ruwan Dias",    message: "Order SL-1006 delivered successfully." },
    ];

    let _eventIdx = 0;
    const nextEvent = () => {
        const evt = realtimeEvents[_eventIdx % realtimeEvents.length];
        _eventIdx++;
        return { ...evt, timestamp: new Date().toLocaleString() };
    };

    return { orders, manifest, routeSteps, trackingTimeline, failReasons, nextEvent };
})();


/*  API SERVICE  –  fetch()-based REST communication */
const ApiService = (() => {

    const BASE = ""; // Will resolve to same origin (e.g. http://localhost:8080)

    /**
     * Generic request wrapper with spinner and error handling.
     * In production this calls the real backend; here it falls
     * back to MockData when the server is unreachable.
     */
    const request = async (method, path, body = null) => {
        const opts = {
            method,
            headers: { "Content-Type": "application/json" },
        };
        if (body) opts.body = JSON.stringify(body);

        try {
            SpinnerCtrl.show();
            const res = await fetch(BASE + path, opts);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return await res.json();
        } catch (err) {
            console.warn(`[ApiService] ${method} ${path} failed – using mock data.`, err.message);
            return null; // caller uses mock fallback
        } finally {
            SpinnerCtrl.hide();
        }
    };

    /* ---- Client endpoints ---- */
    const getOrders = ()         => request("GET", "/api/orders");
    const submitOrder = (data)   => request("POST", "/api/orders", data);
    const trackOrder = (id)      => request("GET", `/api/orders/${id}/track`);

    /* ---- Driver endpoints ---- */
    const getManifest = ()       => request("GET", "/api/driver/manifest");
    const getRoute = ()          => request("GET", "/api/driver/route");
    const updateDelivery = (id, payload) => request("PUT", `/api/driver/deliveries/${id}`, payload);

    return { getOrders, submitOrder, trackOrder, getManifest, getRoute, updateDelivery };
})();


/*  WEBSOCKET SERVICE  –  Real-time update handler (mock) */
const WsService = (() => {

    let ws = null;
    let listeners = [];
    let connected = false;

    /**
     * Attempts to open a WebSocket to the backend.
     * If the server is offline, we fall back to a mock
     * interval-based "push" for demonstration purposes.
     */
    const connect = () => {
        try {
            ws = new WebSocket("ws://localhost:8080/ws");

            ws.onopen = () => {
                connected = true;
                console.log("[WsService] Connected to ws://localhost:8080/ws");
                ToastManager.info("Real-time connection established.");
            };

            ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    _dispatch(data);
                } catch { /* ignore non-JSON frames */ }
            };

            ws.onerror = () => {
                console.warn("[WsService] WebSocket error – falling back to mock mode.");
                connected = false;
            };

            ws.onclose = () => {
                connected = false;
                console.log("[WsService] Disconnected.");
            };

        } catch {
            console.warn("[WsService] WebSocket not available – mock mode active.");
        }
    };

    /** Register a callback for incoming events */
    const onEvent = (fn) => listeners.push(fn);

    /** Dispatch event to all listeners */
    const _dispatch = (data) => {
        listeners.forEach(fn => fn(data));
    };

    /**
     * Simulate an incoming real-time event
     * (used by the "Trigger Event" demo button)
     */
    const simulateEvent = () => {
        const evt = MockData.nextEvent();
        _dispatch(evt);
        return evt;
    };

    return { connect, onEvent, simulateEvent, isConnected: () => connected };
})();


/* TOAST MANAGER  –  Notification system */
const ToastManager = (() => {

    let container;

    const _init = () => {
        if (container) return;
        container = document.createElement("div");
        container.className = "toast-container";
        document.body.appendChild(container);
    };

    const _show = (msg, type = "info", duration = 4000) => {
        _init();
        const icons = { info: "ℹ️", success: "✅", error: "❌", warning: "⚠️" };
        const toast = document.createElement("div");
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <span class="toast-icon">${icons[type] || "ℹ️"}</span>
            <span class="toast-msg">${msg}</span>
            <button class="toast-close" onclick="this.parentElement.remove()">✕</button>
        `;
        container.appendChild(toast);
        setTimeout(() => {
            toast.style.animation = "slideOut .3s ease forwards";
            setTimeout(() => toast.remove(), 300);
        }, duration);
    };

    return {
        info:    (m, d) => _show(m, "info", d),
        success: (m, d) => _show(m, "success", d),
        error:   (m, d) => _show(m, "error", d),
        warning: (m, d) => _show(m, "warning", d),
    };
})();


/*  SPINNER CONTROLLER */
const SpinnerCtrl = (() => {
    let overlay;
    let counter = 0;

    const show = () => {
        counter++;
        if (overlay) { overlay.classList.remove("hidden"); return; }
        overlay = document.createElement("div");
        overlay.className = "spinner-overlay";
        overlay.innerHTML = '<div class="spinner"></div>';
        document.body.appendChild(overlay);
    };

    const hide = () => {
        counter = Math.max(0, counter - 1);
        if (counter === 0 && overlay) overlay.classList.add("hidden");
    };

    return { show, hide };
})();


/* CLIENT APP  –  Dashboard, orders, submission, tracking */
const ClientApp = (() => {

    let orders = [...MockData.orders];

    /* ---- Navigation ---- */
    const initNav = () => {
        document.querySelectorAll(".sidebar-nav a[data-view]").forEach(link => {
            link.addEventListener("click", (e) => {
                e.preventDefault();
                const target = link.dataset.view;
                switchView(target);
                // Update active state
                document.querySelectorAll(".sidebar-nav a").forEach(a => a.classList.remove("active"));
                link.classList.add("active");
                // Close mobile sidebar
                document.querySelector(".sidebar")?.classList.remove("open");
                document.querySelector(".sidebar-overlay")?.classList.remove("show");
            });
        });

        // Mobile menu toggle
        const toggle = document.querySelector(".menu-toggle");
        if (toggle) {
            toggle.addEventListener("click", () => {
                document.querySelector(".sidebar").classList.toggle("open");
                document.querySelector(".sidebar-overlay").classList.toggle("show");
            });
        }

        // Overlay click closes sidebar
        const overlay = document.querySelector(".sidebar-overlay");
        if (overlay) {
            overlay.addEventListener("click", () => {
                document.querySelector(".sidebar").classList.remove("open");
                overlay.classList.remove("show");
            });
        }
    };

    const switchView = (viewId) => {
        document.querySelectorAll(".view-section").forEach(s => s.classList.remove("active"));
        const target = document.getElementById(viewId);
        if (target) target.classList.add("active");
        // Update topbar title
        const titles = {
            "view-dashboard": "Dashboard",
            "view-orders": "My Orders",
            "view-neworder": "Submit New Order",
            "view-tracking": "Order Tracking",
        };
        const tb = document.querySelector(".topbar-title");
        if (tb) tb.textContent = titles[viewId] || "Dashboard";
    };

    /* ---- Dashboard Stats ---- */
    const updateStats = () => {
        const total = orders.length;
        const inTransit = orders.filter(o => o.status === "In Transit").length;
        const delivered = orders.filter(o => o.status === "Delivered").length;
        const pending = orders.filter(o => o.status === "Pending" || o.status === "Processing").length;

        _setText("stat-total", total);
        _setText("stat-transit", inTransit);
        _setText("stat-delivered", delivered);
        _setText("stat-pending", pending);
    };

    /* ---- Render Orders Table ---- */
    const renderOrders = () => {
        const tbody = document.getElementById("orders-tbody");
        if (tbody) {
            tbody.innerHTML = orders.map(o => `
                <tr>
                    <td><strong>${o.id}</strong></td>
                    <td>${o.customer}</td>
                    <td>${o.address}</td>
                    <td>${o.contact}</td>
                    <td><span class="badge badge-${_badgeClass(o.status)}">${o.status}</span></td>
                    <td>${o.priority === "High" ? '<span class="badge badge-failed">High</span>' : 'Normal'}</td>
                    <td>${o.driver}</td>
                    <td>${o.updated}</td>
                </tr>
            `).join("");
        }

        // Dashboard compact table (fewer columns)
        const dashTbody = document.getElementById("dashboard-orders-tbody");
        if (dashTbody) {
            dashTbody.innerHTML = orders.slice(0, 5).map(o => `
                <tr>
                    <td><strong>${o.id}</strong></td>
                    <td>${o.customer}</td>
                    <td><span class="badge badge-${_badgeClass(o.status)}">${o.status}</span></td>
                    <td>${o.driver}</td>
                    <td>${o.updated}</td>
                </tr>
            `).join("");
        }
    };

    /* ---- Submit new order ---- */
    const initOrderForm = () => {
        const form = document.getElementById("order-form");
        if (!form) return;

        form.addEventListener("submit", async (e) => {
            e.preventDefault();

            const data = {
                customer: document.getElementById("inp-name").value.trim(),
                address:  document.getElementById("inp-address").value.trim(),
                contact:  document.getElementById("inp-contact").value.trim(),
                priority: document.getElementById("inp-priority").value,
            };

            if (!data.customer || !data.address || !data.contact) {
                ToastManager.warning("Please fill in all required fields.");
                return;
            }

            // Attempt real API call first
            SpinnerCtrl.show();
            const result = await ApiService.submitOrder(data);

            // Build order object (mock)
            const newOrder = {
                id: `SL-${1009 + orders.length}`,
                ...data,
                status: "Pending",
                driver: "Unassigned",
                updated: new Date().toLocaleString(),
            };

            orders.unshift(newOrder);
            updateStats();
            renderOrders();
            form.reset();
            SpinnerCtrl.hide();

            ToastManager.success(`Order ${newOrder.id} submitted successfully!`);
        });
    };

    /* ---- Tracking (inline in client dashboard) ---- */
    const initTracking = () => {
        const btn = document.getElementById("btn-track");
        if (!btn) return;

        btn.addEventListener("click", async () => {
            const id = document.getElementById("inp-trackId").value.trim();
            if (!id) { ToastManager.warning("Enter an Order ID."); return; }

            SpinnerCtrl.show();
            const result = await ApiService.trackOrder(id);
            SpinnerCtrl.hide();

            // Use mock timeline
            const timeline = MockData.trackingTimeline;
            const container = document.getElementById("tracking-result");
            if (!container) return;

            container.innerHTML = `
                <h4 style="margin-bottom:1rem;">Tracking: ${id}</h4>
                <div class="tracking-timeline">
                    ${timeline.map(t => `
                        <div class="timeline-item ${t.done ? 'done' : ''} ${t.current ? 'current' : ''}">
                            <h4>${t.step}</h4>
                            <p>${t.time || '—'}</p>
                        </div>
                    `).join("")}
                </div>
            `;

            // Also show map placeholder
            const mapEl = document.getElementById("tracking-map");
            if (mapEl) {
                mapEl.innerHTML = `
                    <div class="map-placeholder mt-2">
                        <span class="map-icon">🗺️</span>
                        <span>Live Map – Tracking ${id}</span>
                        <small class="text-dim" style="margin-top:.3rem">Map integration point (Google Maps / Leaflet)</small>
                    </div>
                `;
            }
        });
    };

    /* ---- WebSocket event handler ---- */
    const handleWsEvent = (evt) => {
        // Update order in list
        const idx = orders.findIndex(o => o.id === evt.orderId);
        if (idx >= 0) {
            orders[idx].status = evt.newStatus;
            orders[idx].driver = evt.driver || orders[idx].driver;
            orders[idx].updated = evt.timestamp || new Date().toLocaleString();
        }
        updateStats();
        renderOrders();
        ToastManager.info(`🔔 ${evt.message}`);
    };

    /* ---- Initialise ---- */
    const init = () => {
        initNav();
        updateStats();
        renderOrders();
        initOrderForm();
        initTracking();

        // Listen for real-time updates
        WsService.onEvent(handleWsEvent);
        WsService.connect();

        // Auto-refresh orders via REST every 30s (async polling fallback)
        setInterval(async () => {
            const fresh = await ApiService.getOrders();
            if (fresh && Array.isArray(fresh)) {
                orders = fresh;
                updateStats();
                renderOrders();
            }
        }, 30000);
    };

    /* ---- Helpers ---- */
    const _setText = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    };

    const _badgeClass = (status) => {
        const map = { "Pending": "pending", "Processing": "processing", "In Transit": "intransit", "Delivered": "delivered", "Failed": "failed" };
        return map[status] || "pending";
    };

    return { init };
})();


/* DRIVER APP  –  Manifest, route, POD, signature */
const DriverApp = (() => {

    let manifest = [...MockData.manifest];

    /* ---- Tab Navigation (sidebar layout – unified selectors) ---- */
    const initTabs = () => {
        document.querySelectorAll(".sidebar-nav a[data-view]").forEach(link => {
            link.addEventListener("click", (e) => {
                e.preventDefault();
                const target = link.dataset.view;
                document.querySelectorAll(".sidebar-nav a").forEach(a => a.classList.remove("active"));
                link.classList.add("active");
                document.querySelectorAll(".view-section").forEach(v => v.classList.remove("active"));
                const el = document.getElementById(target);
                if (el) el.classList.add("active");
                // Update topbar title
                const titles = { "drv-deliveries": "Deliveries", "drv-route": "Optimised Route", "drv-pod": "Proof of Delivery" };
                const tb = document.querySelector(".topbar-title");
                if (tb) tb.textContent = titles[target] || "Driver Dashboard";
                // Close mobile sidebar
                document.querySelector(".sidebar")?.classList.remove("open");
                document.querySelector(".sidebar-overlay")?.classList.remove("show");
            });
        });

        // Mobile menu toggle
        const toggle = document.querySelector(".menu-toggle");
        if (toggle) {
            toggle.addEventListener("click", () => {
                document.querySelector(".sidebar").classList.toggle("open");
                document.querySelector(".sidebar-overlay").classList.toggle("show");
            });
        }

        // Overlay click closes sidebar
        const overlay = document.querySelector(".sidebar-overlay");
        if (overlay) {
            overlay.addEventListener("click", () => {
                document.querySelector(".sidebar").classList.remove("open");
                overlay.classList.remove("show");
            });
        }
    };

    /* ---- Render Manifest ---- */
    const renderManifest = () => {
        const container = document.getElementById("manifest-list");
        if (!container) return;

        container.innerHTML = manifest.map((d, i) => `
            <div class="delivery-card ${d.priority === 'High' ? 'urgent' : ''}" id="dcard-${d.id}">
                <div class="delivery-header">
                    <h4>${d.id} – ${d.customer}</h4>
                    <span class="badge badge-${_badgeClass(d.status)}">${d.status}</span>
                </div>
                <div class="delivery-meta">
                    📍 ${d.address}<br>
                    📞 ${d.contact} &nbsp;|&nbsp; ⏰ ETA: ${d.eta}
                    ${d.notes ? `<br>📝 ${d.notes}` : ''}
                </div>
                <div class="delivery-actions">
                    ${d.status !== 'Delivered' && d.status !== 'Failed' ? `
                        <button class="btn btn-success btn-sm" onclick="DriverApp.markDelivered('${d.id}')">✔ Delivered</button>
                        <button class="btn btn-danger btn-sm" onclick="DriverApp.openFailModal('${d.id}')">✘ Failed</button>
                    ` : ''}
                    ${d.status === 'Delivered' ? '<span style="color:var(--success);font-weight:600;font-size:.85rem;">✔ Completed</span>' : ''}
                    ${d.status === 'Failed' ? '<span style="color:var(--danger);font-weight:600;font-size:.85rem;">✘ Failed</span>' : ''}
                </div>
            </div>
        `).join("");
    };

    /* ---- Mark Delivered ---- */
    const markDelivered = async (id) => {
        SpinnerCtrl.show();
        await ApiService.updateDelivery(id, { status: "Delivered" });
        SpinnerCtrl.hide();

        const idx = manifest.findIndex(d => d.id === id);
        if (idx >= 0) manifest[idx].status = "Delivered";
        renderManifest();
        ToastManager.success(`${id} marked as Delivered!`);
    };

    /* ---- Fail Modal ---- */
    let failTargetId = null;

    const openFailModal = (id) => {
        failTargetId = id;
        const modal = document.getElementById("fail-modal");
        if (modal) modal.classList.remove("hidden");
    };

    const closeFailModal = () => {
        const modal = document.getElementById("fail-modal");
        if (modal) modal.classList.add("hidden");
        failTargetId = null;
    };

    const confirmFail = async () => {
        if (!failTargetId) return;
        const reason = document.getElementById("fail-reason")?.value || "Unknown";

        SpinnerCtrl.show();
        await ApiService.updateDelivery(failTargetId, { status: "Failed", reason });
        SpinnerCtrl.hide();

        const idx = manifest.findIndex(d => d.id === failTargetId);
        if (idx >= 0) {
            manifest[idx].status = "Failed";
            manifest[idx].notes = reason;
        }
        renderManifest();
        closeFailModal();
        ToastManager.error(`${failTargetId} marked as Failed – ${reason}`);
    };

    /* ---- Render Route ---- */
    const renderRoute = () => {
        const container = document.getElementById("route-list");
        if (!container) return;

        container.innerHTML = MockData.routeSteps.map(s => `
            <div class="route-step">
                <div class="route-dot ${s.status === 'completed' ? 'completed' : ''} ${s.status === 'current' ? 'current' : ''}"></div>
                <div class="route-step-info">
                    <h4>Stop ${s.seq} – ${s.orderId}</h4>
                    <p>${s.address}</p>
                </div>
            </div>
        `).join("");
    };

    /* ---- Signature Pad ---- */
    let sigCanvas, sigCtx, sigDrawing = false;

    const initSignature = () => {
        sigCanvas = document.getElementById("sig-canvas");
        if (!sigCanvas) return;

        // Set actual pixel dimensions
        const rect = sigCanvas.parentElement.getBoundingClientRect();
        sigCanvas.width = rect.width;
        sigCanvas.height = 160;

        sigCtx = sigCanvas.getContext("2d");
        sigCtx.lineWidth = 2;
        sigCtx.lineCap = "round";
        sigCtx.strokeStyle = "#e2e8f0";

        const getPos = (e) => {
            const r = sigCanvas.getBoundingClientRect();
            const touch = e.touches ? e.touches[0] : e;
            return { x: touch.clientX - r.left, y: touch.clientY - r.top };
        };

        const start = (e) => { sigDrawing = true; const p = getPos(e); sigCtx.beginPath(); sigCtx.moveTo(p.x, p.y); };
        const draw  = (e) => { if (!sigDrawing) return; e.preventDefault(); const p = getPos(e); sigCtx.lineTo(p.x, p.y); sigCtx.stroke(); };
        const stop  = ()  => { sigDrawing = false; };

        sigCanvas.addEventListener("mousedown", start);
        sigCanvas.addEventListener("mousemove", draw);
        sigCanvas.addEventListener("mouseup", stop);
        sigCanvas.addEventListener("mouseleave", stop);
        sigCanvas.addEventListener("touchstart", start, { passive: false });
        sigCanvas.addEventListener("touchmove", draw, { passive: false });
        sigCanvas.addEventListener("touchend", stop);
    };

    const clearSignature = () => {
        if (sigCtx && sigCanvas) sigCtx.clearRect(0, 0, sigCanvas.width, sigCanvas.height);
    };

    /* ---- Photo upload preview ---- */
    const initPhotoUpload = () => {
        const input = document.getElementById("proof-photo");
        const preview = document.getElementById("photo-preview");
        if (!input || !preview) return;

        input.addEventListener("change", () => {
            preview.innerHTML = "";
            if (input.files.length) {
                const reader = new FileReader();
                reader.onload = (e) => {
                    preview.innerHTML = `<img src="${e.target.result}" style="max-width:100%;border-radius:8px;margin-top:.5rem;" alt="Proof photo">`;
                };
                reader.readAsDataURL(input.files[0]);
            }
        });
    };

    /* ---- Notification Banner ---- */
    const showNotification = (msg) => {
        const banner = document.getElementById("notif-banner");
        if (!banner) return;
        banner.textContent = msg;
        banner.classList.add("show");
        setTimeout(() => banner.classList.remove("show"), 4000);
    };

    /* ---- WebSocket handler (driver) ---- */
    const handleWsEvent = (evt) => {
        showNotification(`🔔 ${evt.message}`);
    };

    /* ---- Init ---- */
    const init = () => {
        initTabs();
        renderManifest();
        renderRoute();
        initSignature();
        initPhotoUpload();

        // Fail modal buttons
        const btnConfirmFail = document.getElementById("btn-confirm-fail");
        if (btnConfirmFail) btnConfirmFail.addEventListener("click", confirmFail);
        const btnCancelFail = document.getElementById("btn-cancel-fail");
        if (btnCancelFail) btnCancelFail.addEventListener("click", closeFailModal);

        // Clear signature button
        const btnClearSig = document.getElementById("btn-clear-sig");
        if (btnClearSig) btnClearSig.addEventListener("click", clearSignature);

        // WebSocket
        WsService.onEvent(handleWsEvent);
        WsService.connect();
    };

    /* ---- Helper ---- */
    const _badgeClass = (status) => {
        const map = { "Pending": "pending", "Processing": "processing", "In Transit": "intransit", "Delivered": "delivered", "Failed": "failed" };
        return map[status] || "pending";
    };

    return { init, markDelivered, openFailModal, closeFailModal, confirmFail, clearSignature };
})();


/* TRACK APP  –  Standalone tracking page */
const TrackApp = (() => {

    const init = () => {
        const btn = document.getElementById("btn-track-page");
        if (!btn) return;

        btn.addEventListener("click", async () => {
            const id = document.getElementById("inp-trackId-page").value.trim();
            if (!id) { ToastManager.warning("Enter an Order ID."); return; }

            SpinnerCtrl.show();
            await ApiService.trackOrder(id);
            SpinnerCtrl.hide();

            const timeline = MockData.trackingTimeline;
            const container = document.getElementById("track-result");
            if (!container) return;

            // Find matching order for details
            const order = MockData.orders.find(o => o.id === id) || {
                id, customer: "—", address: "—", status: "Unknown", driver: "—"
            };

            container.innerHTML = `
                <div class="card mb-2">
                    <div class="card-header"><h3>Order ${order.id}</h3></div>
                    <div class="card-body">
                        <p><strong>Customer:</strong> ${order.customer}</p>
                        <p><strong>Address:</strong> ${order.address}</p>
                        <p><strong>Status:</strong> <span class="badge badge-${_badgeClass(order.status)}">${order.status}</span></p>
                        <p><strong>Driver:</strong> ${order.driver}</p>
                    </div>
                </div>

                <div class="card mb-2">
                    <div class="card-header"><h3>Tracking Timeline</h3></div>
                    <div class="card-body">
                        <div class="tracking-timeline">
                            ${timeline.map(t => `
                                <div class="timeline-item ${t.done ? 'done' : ''} ${t.current ? 'current' : ''}">
                                    <h4>${t.step}</h4>
                                    <p>${t.time || '—'}</p>
                                </div>
                            `).join("")}
                        </div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header"><h3>Live Map</h3></div>
                    <div class="card-body">
                        <div class="map-placeholder">
                            <span class="map-icon">🗺️</span>
                            <span>Live Tracking Map – ${order.id}</span>
                            <small class="text-dim" style="margin-top:.3rem;">Map integration point (Google Maps / Leaflet)</small>
                        </div>
                    </div>
                </div>
            `;
        });
    };

    const _badgeClass = (status) => {
        const map = { "Pending": "pending", "Processing": "processing", "In Transit": "intransit", "Delivered": "delivered", "Failed": "failed" };
        return map[status] || "pending";
    };

    return { init };
})();


/* DEMO TOOLBAR  –  Fake real-time event trigger */
const DemoToolbar = (() => {

    const init = () => {
        const btn = document.getElementById("btn-demo-event");
        if (!btn) return;

        btn.addEventListener("click", () => {
            const evt = WsService.simulateEvent();
            console.log("[Demo] Simulated event:", evt);
        });
    };

    return { init };
})();


/* BOOTSTRAP  –  Page-level initialisation */
document.addEventListener("DOMContentLoaded", () => {
    // Detect which page we're on
    const page = document.body.dataset.page;

    if (page === "client") {
        ClientApp.init();
    } else if (page === "driver") {
        DriverApp.init();
    } else if (page === "track") {
        TrackApp.init();
    }

    // Demo toolbar (present on all app pages)
    DemoToolbar.init();
});