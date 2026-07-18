(function () {
    function closeMenu(menu) {
        var toggle = menu.querySelector("[data-profile-toggle]");
        var dropdown = menu.querySelector("[data-profile-dropdown]");
        if (!toggle || !dropdown) {
            return;
        }
        dropdown.hidden = true;
        toggle.setAttribute("aria-expanded", "false");
        menu.classList.remove("is-open");
    }

    function openMenu(menu) {
        var toggle = menu.querySelector("[data-profile-toggle]");
        var dropdown = menu.querySelector("[data-profile-dropdown]");
        if (!toggle || !dropdown) {
            return;
        }
        dropdown.hidden = false;
        toggle.setAttribute("aria-expanded", "true");
        menu.classList.add("is-open");
    }

    document.addEventListener("DOMContentLoaded", function () {
        var menus = document.querySelectorAll("[data-profile-menu]");

        menus.forEach(function (menu) {
            var toggle = menu.querySelector("[data-profile-toggle]");
            if (!toggle) {
                return;
            }

            toggle.addEventListener("click", function (event) {
                event.stopPropagation();
                var isOpen = menu.classList.contains("is-open");
                menus.forEach(closeMenu);
                if (!isOpen) {
                    openMenu(menu);
                }
            });
        });

        document.addEventListener("click", function () {
            menus.forEach(closeMenu);
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                menus.forEach(closeMenu);
            }
        });
    });
})();
