(function () {
    const hostId = "toast-host";

    function ensureHost() {
        let host = document.getElementById(hostId);
        if (!host) {
            host = document.createElement("div");
            host.id = hostId;
            host.className = "toast-host";
            host.setAttribute("aria-live", "polite");
            document.body.appendChild(host);
        }
        return host;
    }

    /** Map HTTP-ish status / aliases to toast type. 2xx → success (green), 4xx/5xx → error (red). */
    function resolveType(typeOrStatus) {
        if (typeOrStatus == null || typeOrStatus === "") return "info";
        if (typeof typeOrStatus === "number") {
            if (typeOrStatus >= 200 && typeOrStatus < 300) return "success";
            if (typeOrStatus >= 400) return "error";
            return "info";
        }
        const raw = String(typeOrStatus).toLowerCase().trim();
        if (raw === "success" || raw === "ok" || raw === "200") return "success";
        if (raw === "error" || raw === "danger" || raw === "402" || raw === "400" || raw === "401" || raw === "403" || raw === "404" || raw === "500") {
            return "error";
        }
        if (raw === "info" || raw === "warning") return raw === "warning" ? "error" : "info";
        return raw;
    }

    function showToast(message, typeOrStatus) {
        if (!message) return;
        const type = resolveType(typeOrStatus);
        const host = ensureHost();
        const toast = document.createElement("div");
        toast.className = "toast toast-" + type;
        toast.setAttribute("role", "status");
        toast.innerHTML =
            '<div class="toast-body">' +
            '<span class="toast-msg"></span>' +
            '<button type="button" class="toast-close" aria-label="Close">&times;</button>' +
            "</div>";
        toast.querySelector(".toast-msg").textContent = message;

        const remove = () => {
            toast.classList.add("toast-out");
            setTimeout(() => toast.remove(), 280);
        };

        toast.querySelector(".toast-close").addEventListener("click", remove);
        host.appendChild(toast);
        requestAnimationFrame(() => toast.classList.add("toast-in"));
        setTimeout(remove, 4200);
    }

    window.showToast = showToast;

    document.addEventListener("DOMContentLoaded", () => {
        const success = document.body.dataset.flashSuccess;
        const error = document.body.dataset.flashError;
        if (success) showToast(success, 200);
        if (error) showToast(error, 402);
    });
})();
