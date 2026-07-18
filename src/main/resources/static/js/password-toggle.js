(function () {
    function initPasswordToggle(buttonId, inputId) {
        var button = document.getElementById(buttonId);
        var input = document.getElementById(inputId);
        if (!button || !input) {
            return;
        }

        var eye = button.querySelector(".icon-eye");
        var eyeOff = button.querySelector(".icon-eye-off");

        button.addEventListener("click", function () {
            var showing = input.type === "text";
            input.type = showing ? "password" : "text";
            button.setAttribute("aria-pressed", showing ? "false" : "true");
            button.setAttribute("aria-label", showing ? "Show password" : "Hide password");
            button.setAttribute("title", showing ? "Show password" : "Hide password");

            if (eye && eyeOff) {
                eye.hidden = !showing;
                eyeOff.hidden = showing;
            }
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        initPasswordToggle("togglePassword", "password");
    });
})();
