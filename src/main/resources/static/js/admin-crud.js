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

    // User form: first/last name flow — grade for STUDENT, then classes by grade; room+classes for TEACHER
    function roleNameOf(form) {
        const select = qs("[data-role-select]", form);
        if (!select) return "";
        const option = select.options[select.selectedIndex];
        return option ? option.getAttribute("data-role-name") || "" : "";
    }

    function syncRoleFields(form) {
        const roleName = roleNameOf(form);
        const gradeField = qs("[data-field-grade]", form);
        const roomField = qs("[data-field-room]", form);
        const classField = qs("[data-field-class]", form);
        const gradeSelect = qs("[data-grade-select]", form);
        const schoolSelect = qs("[data-school-select]", form);
        const classSelect = qs("[data-class-select]", form);
        const hint = qs("[data-class-hint]", form);

        const isStudent = roleName === "STUDENT";
        const isTeacher = roleName === "TEACHER";

        if (gradeField) gradeField.hidden = !isStudent;
        if (roomField) roomField.hidden = !isTeacher;

        const gradeValue = gradeSelect ? gradeSelect.value : "";
        const schoolValue = schoolSelect ? schoolSelect.value : "";

        let showClasses = false;
        if (isTeacher) {
            showClasses = true;
            if (hint) hint.textContent = "Select teaching classes for this teacher.";
        } else if (isStudent && gradeValue) {
            showClasses = true;
            if (hint) hint.textContent = "Classes matching the selected grade (and school). Hold Ctrl/Cmd for multiple.";
        } else if (isStudent) {
            if (hint) hint.textContent = "Select a grade first to see matching classes.";
        } else if (classField) {
            if (hint) hint.textContent = "Classes are only for Student or Teacher roles.";
        }

        if (classField) classField.hidden = !showClasses;

        if (classSelect) {
            Array.from(classSelect.options).forEach((opt) => {
                if (!opt.value) return;
                const optGrade = (opt.getAttribute("data-class-grade") || "").trim();
                const optSchool = opt.getAttribute("data-class-school") || "";
                let visible = true;
                if (schoolValue && optSchool && optSchool !== schoolValue) {
                    visible = false;
                }
                if (isStudent) {
                    visible = visible && !!gradeValue
                        && optGrade.toLowerCase() === gradeValue.trim().toLowerCase();
                }
                opt.hidden = !visible;
                opt.disabled = !visible;
                if (!visible && opt.selected) {
                    opt.selected = false;
                }
            });
        }
    }

    qsa("form[data-role-form]").forEach((form) => {
        const roleSelect = qs("[data-role-select]", form);
        const gradeSelect = qs("[data-grade-select]", form);
        const schoolSelect = qs("[data-school-select]", form);
        if (!roleSelect) return;
        syncRoleFields(form);
        roleSelect.addEventListener("change", () => syncRoleFields(form));
        if (gradeSelect) gradeSelect.addEventListener("change", () => syncRoleFields(form));
        if (schoolSelect) schoolSelect.addEventListener("change", () => syncRoleFields(form));
    });

    // Permission assign/replace/revoke: reload with roleUuid so current permissions auto-tick
    qsa("form[data-permission-form] [data-role-permission-select]").forEach((select) => {
        select.addEventListener("change", () => {
            const roleUuid = select.value;
            if (!roleUuid) return;
            const url = new URL(window.location.href);
            url.searchParams.set("roleUuid", roleUuid);
            window.location.href = url.toString();
        });
    });

    // Generic table/panel swap switch (finance, grades, etc.)
    qsa("[data-view-switch]").forEach((switchEl) => {
        const root = switchEl.closest("section") || document;
        const buttons = qsa("[data-view-target]", switchEl);
        const panels = qsa("[data-view-panel]", root);
        const searchInput = qs("[data-view-search]", root) || qs("[data-finance-search]", root);
        const targets = buttons.map((btn) => btn.getAttribute("data-view-target")).filter(Boolean);

        function activate(target) {
            if (!targets.includes(target)) return;
            buttons.forEach((btn) => {
                const isActive = btn.getAttribute("data-view-target") === target;
                btn.classList.toggle("is-active", isActive);
                btn.setAttribute("aria-selected", isActive ? "true" : "false");
            });
            panels.forEach((panel) => {
                const match = panel.getAttribute("data-view-panel") === target;
                panel.hidden = !match;
            });

            const activeBtn = buttons.find((btn) => btn.getAttribute("data-view-target") === target);
            const searchTableId =
                (activeBtn && activeBtn.getAttribute("data-search-table")) ||
                (qs(`[data-view-panel="${target}"]`, root) || {}).getAttribute?.("data-search-table");
            if (searchInput && searchTableId) {
                searchInput.setAttribute("data-table-search", searchTableId);
                searchInput.value = "";
                const table = document.getElementById(searchTableId);
                if (table) {
                    qsa("tbody tr[data-search]", table).forEach((row) => {
                        row.hidden = false;
                    });
                }
            }

            qsa("[data-view-action]", root).forEach((action) => {
                const forTarget = action.getAttribute("data-view-action");
                action.hidden = forTarget !== target;
            });

            try {
                const url = new URL(window.location.href);
                url.searchParams.set("tab", target);
                window.history.replaceState({}, "", url.toString());
            } catch (_) {
                /* ignore */
            }
        }

        buttons.forEach((btn) => {
            btn.addEventListener("click", () => activate(btn.getAttribute("data-view-target")));
        });

        const params = new URLSearchParams(window.location.search);
        const fromUrl = params.get("tab");
        const initial =
            (fromUrl && targets.includes(fromUrl) && fromUrl) ||
            switchEl.getAttribute("data-view-default") ||
            targets[0];
        if (initial) activate(initial);
    });
})();
