(function () {
    var STORAGE_KEY = "sm.notifications.v1";
    var filter = "unread";

    var TYPE_PATHS = {
        user: "/admin/users",
        class: "/admin/classes",
        school: "/admin/schools",
        score: "/admin/scores",
        grade: "/admin/grades",
        attendance: "/admin/attendance",
        request: "/admin/requests",
        finance: "/admin/finance",
        role: "/admin/roles",
        permission: "/admin/permissions",
        welcome: "/admin/dashboard"
    };

    var DEFAULT_ITEMS = [
        {
            id: "n1",
            type: "user",
            title: "New user registered",
            body: "A new account was created and is ready for role assignment.",
            at: "2026-07-19T09:35:08",
            read: false,
            href: "/admin/users"
        },
        {
            id: "n2",
            type: "welcome",
            title: "Welcome to School Management",
            body: "Your dashboard is ready. Explore schools, users, classes, and roles.",
            at: "2026-07-19T09:35:07",
            read: false,
            href: "/admin/dashboard"
        },
        {
            id: "n3",
            type: "class",
            title: "Class generation updated",
            body: "A class generation year was changed for the current school year.",
            at: "2026-07-18T14:12:00",
            read: true,
            href: "/admin/classes"
        },
        {
            id: "n4",
            type: "school",
            title: "School profile updated",
            body: "Logo or banner was updated for a school profile.",
            at: "2026-07-17T11:05:22",
            read: true,
            href: "/admin/schools"
        }
    ];

    function qs(sel, root) {
        return (root || document).querySelector(sel);
    }

    function qsa(sel, root) {
        return Array.from((root || document).querySelectorAll(sel));
    }

    function resolveHref(item) {
        if (!item) return "/admin/dashboard";
        if (item.href) return item.href;
        return TYPE_PATHS[item.type] || "/admin/dashboard";
    }

    function loadItems() {
        try {
            var raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) return DEFAULT_ITEMS.map(clone);
            var parsed = JSON.parse(raw);
            if (!Array.isArray(parsed) || !parsed.length) return DEFAULT_ITEMS.map(clone);
            // Backfill href for older stored items
            return parsed.map(function (item) {
                var next = clone(item);
                if (!next.href) next.href = resolveHref(next);
                return next;
            });
        } catch (e) {
            return DEFAULT_ITEMS.map(clone);
        }
    }

    function clone(item) {
        return Object.assign({}, item);
    }

    function saveItems(items) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    }

    function formatWhen(iso) {
        try {
            return new Date(iso).toLocaleString(undefined, {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
                hour12: false
            });
        } catch (e) {
            return iso;
        }
    }

    function iconSvg(type) {
        if (type === "user") {
            return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>';
        }
        if (type === "class") {
            return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>';
        }
        if (type === "school") {
            return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 21h18"/><path d="M5 21V7l7-4 7 4v14"/><path d="M9 21v-6h6v6"/></svg>';
        }
        return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>';
    }

    function render() {
        var items = loadItems();
        var list = qs("[data-notif-list]");
        var badge = qs("[data-notif-badge]");
        if (!list) return;

        var unreadCount = items.filter(function (i) { return !i.read; }).length;
        if (badge) {
            if (unreadCount > 0) {
                badge.hidden = false;
                badge.textContent = unreadCount > 9 ? "9+" : String(unreadCount);
            } else {
                badge.hidden = true;
            }
        }

        var visible = items.filter(function (i) {
            if (filter === "all") return true;
            return filter === "unread" ? !i.read : i.read;
        });

        if (!visible.length) {
            list.innerHTML =
                '<div class="notif-empty">' +
                "<p>No " + (filter === "all" ? "" : filter + " ") + "notifications</p>" +
                "</div>";
            return;
        }

        list.innerHTML = visible
            .map(function (item) {
                var href = resolveHref(item);
                return (
                    '<article class="notif-item' + (item.read ? " is-read" : " is-unread") +
                    '" data-notif-id="' + item.id + '" data-notif-href="' + escapeHtml(href) +
                    '" role="link" tabindex="0">' +
                    '<div class="notif-icon notif-icon-' + item.type + '">' + iconSvg(item.type) + "</div>" +
                    '<div class="notif-content">' +
                    '<div class="notif-title-row">' +
                    '<strong>' + escapeHtml(item.title) + "</strong>" +
                    '<span class="notif-status-dot' + (item.read ? " read" : " unread") + '"></span>' +
                    "</div>" +
                    '<p>' + escapeHtml(item.body) + "</p>" +
                    '<time>' + escapeHtml(formatWhen(item.at)) + "</time>" +
                    "</div>" +
                    "</article>"
                );
            })
            .join("");

        qsa(".notif-item", list).forEach(function (el) {
            el.addEventListener("click", function () {
                openNotification(el.getAttribute("data-notif-id"), el.getAttribute("data-notif-href"));
            });
            el.addEventListener("keydown", function (e) {
                if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    openNotification(el.getAttribute("data-notif-id"), el.getAttribute("data-notif-href"));
                }
            });
        });
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function markRead(id) {
        var items = loadItems();
        var changed = false;
        items.forEach(function (item) {
            if (item.id === id && !item.read) {
                item.read = true;
                changed = true;
            }
        });
        if (changed) {
            saveItems(items);
        }
        return changed;
    }

    function openNotification(id, href) {
        markRead(id);
        var target = href || "/admin/dashboard";
        closeDrawer();
        // Navigate after marking unread → read
        window.location.href = target;
    }

    function markAllRead() {
        var items = loadItems().map(function (item) {
            item.read = true;
            return item;
        });
        saveItems(items);
        render();
    }

    function openDrawer() {
        var drawer = qs("[data-notifications-drawer]");
        var backdrop = qs("[data-notifications-backdrop]");
        if (!drawer) return;
        drawer.hidden = false;
        if (backdrop) backdrop.hidden = false;
        requestAnimationFrame(function () {
            document.body.classList.add("notif-open");
        });
        render();
    }

    function closeDrawer() {
        var drawer = qs("[data-notifications-drawer]");
        var backdrop = qs("[data-notifications-backdrop]");
        document.body.classList.remove("notif-open");
        setTimeout(function () {
            if (drawer) drawer.hidden = true;
            if (backdrop) backdrop.hidden = true;
        }, 220);
    }

    document.addEventListener("DOMContentLoaded", function () {
        if (!localStorage.getItem(STORAGE_KEY)) {
            saveItems(DEFAULT_ITEMS.map(clone));
        } else {
            // Persist backfilled hrefs once
            saveItems(loadItems());
        }

        qsa("[data-notifications-open]").forEach(function (btn) {
            btn.addEventListener("click", function (e) {
                e.stopPropagation();
                openDrawer();
            });
        });

        qsa("[data-notifications-close], [data-notifications-backdrop]").forEach(function (el) {
            el.addEventListener("click", closeDrawer);
        });

        var markAll = qs("[data-notif-mark-all]");
        if (markAll) markAll.addEventListener("click", markAllRead);

        var viewAll = qs("[data-notif-view-all]");
        if (viewAll) {
            viewAll.addEventListener("click", function () {
                filter = "all";
                qsa("[data-notif-filter]").forEach(function (btn) {
                    btn.classList.remove("is-active");
                    btn.setAttribute("aria-selected", "false");
                });
                render();
            });
        }

        qsa("[data-notif-filter]").forEach(function (btn) {
            btn.addEventListener("click", function () {
                filter = btn.getAttribute("data-notif-filter") || "unread";
                qsa("[data-notif-filter]").forEach(function (b) {
                    var active = b === btn;
                    b.classList.toggle("is-active", active);
                    b.setAttribute("aria-selected", active ? "true" : "false");
                });
                render();
            });
        });

        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && document.body.classList.contains("notif-open")) {
                closeDrawer();
            }
        });

        render();
    });
})();
