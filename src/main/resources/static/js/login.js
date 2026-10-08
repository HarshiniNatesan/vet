async function login() {
    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const role = document.getElementById("role").value;
    const msg = document.getElementById("msg");
    const button = document.querySelector(".login-button");

    msg.textContent = "";

    if (!username || !password) {
        msg.textContent = "Please enter your username and password.";
        return;
    }

    button.disabled = true;
    button.textContent = "Signing in...";

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password, role })
        });

        const text = await response.text();
        let data = {};
        try { data = text ? JSON.parse(text) : {}; } catch (_) {}

        if (!response.ok) {
            throw new Error(data.message || data.error || "Login failed.");
        }

        // Store only display/navigation information. Authorization is enforced by Spring Security.
        localStorage.setItem("username", data.username);
        localStorage.setItem("role", data.role);

        const dashboard = {
            PET_OWNER: "/owner.html",
            VETERINARIAN: "/vet.html",
            RECEPTIONIST: "/reception.html",
            PHARMACY: "/pharmacy.html",
            ADMIN: "/admin.html"
        }[data.role];

        if (!dashboard) {
            throw new Error("Your account does not have a valid system role.");
        }

        window.location.replace(dashboard);
    } catch (error) {
        msg.textContent = error.message || "Unable to log in.";
    } finally {
        button.disabled = false;
        button.textContent = "Login";
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const password = document.getElementById("password");
    if (password) {
        password.addEventListener("keydown", event => {
            if (event.key === "Enter") login();
        });
    }
});
