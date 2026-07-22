(function () {
    var MQ = window.matchMedia("(max-width: 860px)");

    function qs(sel, root) {
        return (root || document).querySelector(sel);
    }

    function qsa(sel, root) {
        return Array.prototype.slice.call((root || document).querySelectorAll(sel));
    }

    function isMobile() {
        return MQ.matches;
    }

    function setToggleState(open) {
        qsa("[data-sidebar-toggle]").forEach(function (btn) {
            btn.setAttribute("aria-expanded", open ? "true" : "false");
            var openIcon = qs(".sidebar-toggle-open", btn);
            var closeIcon = qs(".sidebar-toggle-close", btn);
            if (openIcon) openIcon.hidden = open;
            if (closeIcon) closeIcon.hidden = !open;
            var labelKey = open ? "header.closeMenu" : "header.openMenu";
            btn.setAttribute("data-i18n-aria", labelKey);
            btn.setAttribute("data-i18n-title", labelKey);
            var label = open ? "Close menu" : "Open menu";
            btn.setAttribute("aria-label", label);
            btn.setAttribute("title", label);
        });
    }

    function openSidebar() {
        var backdrop = qs("[data-sidebar-backdrop]");
        if (backdrop) backdrop.hidden = false;
        requestAnimationFrame(function () {
            document.body.classList.add("sidebar-open");
        });
        setToggleState(true);
        document.body.style.overflow = "hidden";
    }

    function closeSidebar() {
        document.body.classList.remove("sidebar-open");
        setToggleState(false);
        document.body.style.overflow = "";
        setTimeout(function () {
            if (!document.body.classList.contains("sidebar-open")) {
                var backdrop = qs("[data-sidebar-backdrop]");
                if (backdrop) backdrop.hidden = true;
            }
        }, 240);
    }

    function toggleSidebar() {
        if (document.body.classList.contains("sidebar-open")) {
            closeSidebar();
        } else {
            openSidebar();
        }
    }

    function syncGroupsForViewport() {
        qsa("[data-sidebar-group]").forEach(function (group) {
            if (!isMobile()) {
                group.open = true;
                return;
            }
            var hasActive = !!qs("a.active", group);
            group.open = hasActive;
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        qsa("[data-sidebar-toggle]").forEach(function (btn) {
            btn.addEventListener("click", function (e) {
                e.stopPropagation();
                if (!isMobile()) return;
                toggleSidebar();
            });
        });

        qsa("[data-sidebar-close], [data-sidebar-backdrop]").forEach(function (el) {
            el.addEventListener("click", function () {
                if (isMobile()) closeSidebar();
            });
        });

        qsa("[data-sidebar] a").forEach(function (link) {
            link.addEventListener("click", function () {
                if (isMobile()) closeSidebar();
            });
        });

        qsa("[data-sidebar-group] > .sidebar-label").forEach(function (summary) {
            summary.addEventListener("click", function (e) {
                if (!isMobile()) {
                    e.preventDefault();
                }
            });
        });

        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && document.body.classList.contains("sidebar-open")) {
                closeSidebar();
            }
        });

        syncGroupsForViewport();

        var onMqChange = function () {
            if (!isMobile()) {
                closeSidebar();
                syncGroupsForViewport();
            } else {
                syncGroupsForViewport();
            }
        };

        if (typeof MQ.addEventListener === "function") {
            MQ.addEventListener("change", onMqChange);
        } else if (typeof MQ.addListener === "function") {
            MQ.addListener(onMqChange);
        }
    });
})();
