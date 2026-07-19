(function () {
    function qs(sel, root) {
        return (root || document).querySelector(sel);
    }

    function qsa(sel, root) {
        return Array.from((root || document).querySelectorAll(sel));
    }

    function bindTile(tile) {
        const input = qs("[data-upload-input]", tile);
        const preview = qs("[data-upload-preview]", tile);
        const placeholder = qs("[data-upload-placeholder]", tile);
        const defaultImg = qs("[data-upload-default]", tile);
        const filename = qs("[data-upload-filename]", tile);
        const triggers = qsa("[data-upload-trigger]", tile);
        if (!input || !preview) return;

        function openPicker(e) {
            if (e) e.preventDefault();
            input.click();
        }

        triggers.forEach((el) => el.addEventListener("click", openPicker));

        preview.addEventListener("dragover", (e) => {
            e.preventDefault();
            tile.classList.add("is-dragover");
        });
        preview.addEventListener("dragleave", () => tile.classList.remove("is-dragover"));
        preview.addEventListener("drop", (e) => {
            e.preventDefault();
            tile.classList.remove("is-dragover");
            const file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
            if (!file) return;
            const dt = new DataTransfer();
            dt.items.add(file);
            input.files = dt.files;
            input.dispatchEvent(new Event("change", { bubbles: true }));
        });

        input.addEventListener("change", () => {
            const file = input.files && input.files[0];
            if (!file) return;
            if (!file.type.startsWith("image/")) {
                if (window.showToast) window.showToast("Please choose an image file", "error");
                input.value = "";
                return;
            }
            if (file.size > 5 * 1024 * 1024) {
                if (window.showToast) window.showToast("Image must be 5MB or smaller", "error");
                input.value = "";
                return;
            }
            const url = URL.createObjectURL(file);
            preview.style.backgroundImage = "url('" + url + "')";
            preview.classList.add("has-image");
            preview.classList.remove("has-default");
            if (placeholder) placeholder.hidden = true;
            if (defaultImg) defaultImg.hidden = true;
            if (filename) filename.textContent = file.name;
        });
    }

    qsa("[data-upload-tile]").forEach(bindTile);
})();
