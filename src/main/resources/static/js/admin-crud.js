(function () {
    function qs(sel, root) {
        return (root || document).querySelector(sel);
    }

    function qsa(sel, root) {
        return Array.from((root || document).querySelectorAll(sel));
    }

    // Table search
    qsa("[data-table-search]").forEach((input) => {
        const table = document.getElementById(input.getAttribute("data-table-search"));
        if (!table) return;
        input.addEventListener("input", () => {
            const q = input.value.trim().toLowerCase();
            qsa("tbody tr[data-search]", table).forEach((row) => {
                const hay = (row.getAttribute("data-search") || "").toLowerCase();
                row.hidden = q !== "" && !hay.includes(q);
            });
        });
    });

    // Confirm modal (delete only)
    const confirmModal = qs("#confirm-modal");
    const confirmForm = qs("#confirm-form");
    const confirmMsg = qs("#confirm-message");

    function openConfirm(action, message) {
        if (!confirmModal || !confirmForm) return;
        confirmForm.setAttribute("action", action);
        if (confirmMsg) confirmMsg.textContent = message || "Are you sure?";
        confirmModal.hidden = false;
        document.body.classList.add("modal-open");
    }

    function closeConfirm() {
        if (!confirmModal) return;
        confirmModal.hidden = true;
        document.body.classList.remove("modal-open");
    }

    document.addEventListener("click", (e) => {
        const openBtn = e.target.closest("[data-confirm-open]");
        if (openBtn) {
            e.preventDefault();
            openConfirm(
                openBtn.getAttribute("data-confirm-action"),
                openBtn.getAttribute("data-confirm-message")
            );
            return;
        }
        if (e.target.closest("[data-confirm-close]") || e.target === confirmModal) {
            e.preventDefault();
            closeConfirm();
        }
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") closeConfirm();
    });

    // User form: show grade for STUDENT, room for TEACHER
    function syncRoleFields(form) {
        const select = qs("[data-role-select]", form);
        if (!select) return;
        const option = select.options[select.selectedIndex];
        const roleName = option ? option.getAttribute("data-role-name") : "";
        const grade = qs("[data-field-grade]", form);
        const room = qs("[data-field-room]", form);
        if (grade) grade.hidden = roleName !== "STUDENT";
        if (room) room.hidden = roleName !== "TEACHER";
    }

    qsa("form[data-role-form]").forEach((form) => {
        const select = qs("[data-role-select]", form);
        if (!select) return;
        syncRoleFields(form);
        select.addEventListener("change", () => syncRoleFields(form));
    });
})();
