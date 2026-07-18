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

    function showToast(message, type) {
        if (!message) return;
        const host = ensureHost();
        const toast = document.createElement("div");
        toast.className = "toast toast-" + (type || "info");
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
        if (success) showToast(success, "success");
        if (error) showToast(error, "error");
    });
})();
