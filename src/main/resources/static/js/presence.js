(function () {
    function applyPresence(onlineEmails) {
        const set = new Set(onlineEmails || []);
        document.querySelectorAll("[data-presence-email]").forEach((el) => {
            const email = el.getAttribute("data-presence-email");
            const online = set.has(email);
            el.classList.toggle("online", online);
            el.classList.toggle("offline", !online);
            const label = el.querySelector(".presence-label");
            if (label) label.textContent = online ? "Online" : "Offline";
        });
        const countEl = document.querySelector("[data-online-count]");
        if (countEl) countEl.textContent = String(set.size);
    }

    function connect() {
        if (typeof SockJS === "undefined" || typeof Stomp === "undefined") {
            return;
        }
        const socket = new SockJS("/ws");
        const client = Stomp.over(socket);
        client.debug = null;
        client.connect({}, function () {
            client.subscribe("/topic/presence", function (message) {
                try {
                    const payload = JSON.parse(message.body);
                    applyPresence(payload.onlineEmails || []);
                } catch (e) {
                    /* ignore bad payload */
                }
            });
            // Register this session for real-time presence
            client.send("/app/presence/ping", {}, "{}");
            fetch("/api/presence/online", { credentials: "same-origin" })
                .then((r) => (r.ok ? r.json() : null))
                .then((body) => {
                    if (body && body.data) {
                        applyPresence(body.data.onlineEmails || []);
                    }
                })
                .catch(() => {});
        }, function () {
            setTimeout(connect, 4000);
        });
    }

    document.addEventListener("DOMContentLoaded", connect);
})();
