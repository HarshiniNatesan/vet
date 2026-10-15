/* PawsCare UI shell: icons, sidebar polish, mobile menu, status badges.
   Presentation only. It never touches authentication or business logic. */
(function () {
    "use strict";

    var P = {
        grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/>',
        calendar: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>',
        users: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
        user: '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
        userplus: '<path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M20 8v6M23 11h-6"/>',
        file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>',
        clock: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
        chart: '<path d="M12 20V10M18 20V4M6 20v-4"/>',
        lock: '<rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>',
        card: '<rect x="1" y="4" width="22" height="16" rx="2"/><path d="M1 10h22"/>',
        building: '<path d="M3 21h18M5 21V7l8-4v18M19 21V11l-6-4"/><path d="M9 9v.01M9 12v.01M9 15v.01M9 18v.01"/>',
        box: '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><path d="M3.27 6.96 12 12.01l8.73-5.05M12 22.08V12"/>',
        search: '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>',
        alert: '<path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><path d="M12 9v4M12 17h.01"/>',
        activity: '<path d="M22 12h-4l-3 9L9 3l-3 9H2"/>',
        scissors: '<circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M20 4 8.12 15.88M14.47 14.48 20 20M8.12 8.12 12 12"/>',
        clipboard: '<path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/>',
        steth: '<path d="M6 3v6a4 4 0 0 0 8 0V3"/><path d="M10 13v2a5 5 0 0 0 10 0v-1"/><circle cx="20" cy="12" r="2"/>',
        syringe: '<path d="m18 2 4 4M17 7l3-3M19 9l-8.7 8.7a2.5 2.5 0 0 1-3.5 0l-.5-.5a2.5 2.5 0 0 1 0-3.5L15 5M12 8l4 4M5 19l-3 3"/>',
        pill: '<path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7z"/><path d="m8.5 8.5 7 7"/>',
        paw: '<circle cx="5.5" cy="11" r="2"/><circle cx="9.5" cy="6" r="2"/><circle cx="14.5" cy="6" r="2"/><circle cx="18.5" cy="11" r="2"/><path d="M12 12c-3 0-6 4-5 6.5 1 2 3 1.5 5 1.5s4 .5 5-1.5C18 16 15 12 12 12z"/>',
        shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
        menu: '<path d="M3 12h18M3 6h18M3 18h18"/>',
        eye: '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>',
        check: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="M22 4 12 14.01l-3-3"/>',
        heart: '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>',
        up: '<path d="M7 17 17 7M8 7h9v9"/>'
    };

    function icon(name, cls) {
        return '<svg class="ui-icon ' + (cls || "") + '" viewBox="0 0 24 24" aria-hidden="true">' + (P[name] || P.grid) + "</svg>";
    }
    window.PawsIcon = icon;

    var RULES = [
        [/password|security/, "lock"],
        [/billing|payment/, "card"],
        [/branch/, "building"],
        [/statistic|report/, "chart"],
        [/alert/, "alert"],
        [/dispens|prescription/, "pill"],
        [/medicine|stock|inventory/, "box"],
        [/vaccin/, "syringe"],
        [/groom|spa/, "scissors"],
        [/availab/, "clock"],
        [/appointment/, "calendar"],
        [/medical|record/, "file"],
        [/operation/, "activity"],
        [/register|registration/, "userplus"],
        [/veterinarian|doctor/, "steth"],
        [/receptionist/, "clipboard"],
        [/owner|user/, "users"],
        [/patient|pets?\b/, "paw"],
        [/search|find|lookup/, "search"],
        [/profile/, "user"],
        [/dashboard|home|overview/, "grid"]
    ];

    function iconFor(text) {
        var t = String(text || "").toLowerCase();
        for (var i = 0; i < RULES.length; i++) if (RULES[i][0].test(t)) return RULES[i][1];
        return "grid";
    }

    var ROLE_LABEL = {
        ADMIN: "Administrator", RECEPTIONIST: "Receptionist", VETERINARIAN: "Veterinarian",
        PHARMACY: "Pharmacy", PET_OWNER: "Pet Owner"
    };

    function initials(name) {
        var parts = String(name || "?").replace(/[^A-Za-z0-9 ._-]/g, "").split(/[ ._-]+/).filter(Boolean);
        return ((parts[0] || "?")[0] + (parts[1] ? parts[1][0] : (parts[0] || "")[1] || "")).toUpperCase();
    }

    function safeGet(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }

    function buildBrand(sidebar) {
        var old = sidebar.querySelector(".role-brand");
        if (!old) return;
        var wrap = document.createElement("div");
        wrap.className = "side-brand";
        wrap.innerHTML = '<div class="brand-mark">' + icon("paw") + '</div><div class="side-brand-text"><strong>PawsCare</strong><small>Veterinary Hospital</small></div>';
        old.replaceWith(wrap);
    }

    function buildFooter(sidebar) {
        var logout = sidebar.querySelector(".role-logout");
        if (!logout || sidebar.querySelector(".side-footer")) return;
        var name = safeGet("username") || "Signed in";
        var role = ROLE_LABEL[safeGet("role")] || "";
        var footer = document.createElement("div");
        footer.className = "side-footer";
        footer.innerHTML =
            '<div class="side-user"><div class="av">' + initials(name) + '</div><div class="side-user-text"><strong></strong><span></span></div></div>';
        footer.querySelector("strong").textContent = name;
        footer.querySelector("span").textContent = role;
        logout.textContent = "Sign out";
        logout.parentNode.insertBefore(footer, logout);
        footer.appendChild(logout);
    }

    function decorateNav(root) {
        root.querySelectorAll(".role-nav a, .ph-nav a").forEach(function (a) {
            if (a.querySelector(".ui-icon")) return;
            var old = a.querySelector(".ico");
            var label = (a.textContent || "").trim();
            if (old) old.remove();
            a.insertAdjacentHTML("afterbegin", icon(iconFor(label)));
        });
    }

    function decorateCards() {
        document.querySelectorAll(".summary-card").forEach(function (card) {
            var holder = card.querySelector(".icon");
            if (!holder || holder.querySelector(".ui-icon")) return;
            var h = card.querySelector("h3");
            holder.innerHTML = icon(iconFor(h ? h.textContent : ""));
        });
        document.querySelectorAll(".action-card .mini-ico").forEach(function (holder) {
            if (holder.querySelector(".ui-icon")) return;
            var card = holder.closest(".action-card");
            var h = card && card.querySelector("h4");
            holder.innerHTML = icon(iconFor(h ? h.textContent : ""));
        });
        document.querySelectorAll(".ph-brand-mark").forEach(function (m) {
            if (!m.querySelector(".ui-icon")) m.innerHTML = icon("paw");
        });
        document.querySelectorAll(".stat-card i").forEach(function (i) {
            if (!i.querySelector(".ui-icon")) i.innerHTML = icon("up");
        });
    }


    /* Lay out runs of plain inputs (3 or more in a row) as a two-column grid. */
    function groupFields() {
        document.querySelectorAll(".card").forEach(function (card) {
            var run = [];
            function flush() {
                if (run.length >= 3) {
                    var wrap = document.createElement("div");
                    wrap.className = "field-grid";
                    run[0].parentNode.insertBefore(wrap, run[0]);
                    run.forEach(function (el) { wrap.appendChild(el); });
                }
                run = [];
            }
            Array.prototype.slice.call(card.children).forEach(function (el) {
                var tag = el.tagName;
                var plain = (tag === "INPUT" && el.type !== "hidden" && el.type !== "checkbox") || tag === "SELECT" || tag === "TEXTAREA";
                if (plain) run.push(el);
                else if (tag === "INPUT" && el.type === "hidden") return;
                else flush();
            });
            flush();
        });
    }

    function topbarAvatar() {
        document.querySelectorAll(".top-actions .avatar").forEach(function (a) {
            var n = safeGet("username");
            if (n) a.textContent = initials(n);
        });
    }

    function mobileMenu() {
        if (!document.querySelector(".role-sidebar, .ph-sidebar") || document.querySelector(".menu-toggle")) return;
        var btn = document.createElement("button");
        btn.type = "button";
        btn.className = "menu-toggle";
        btn.setAttribute("aria-label", "Open navigation");
        btn.innerHTML = icon("menu");
        var scrim = document.createElement("div");
        scrim.className = "scrim";
        document.body.appendChild(btn);
        document.body.appendChild(scrim);
        function toggle() { document.body.classList.toggle("nav-open"); }
        btn.addEventListener("click", toggle);
        scrim.addEventListener("click", toggle);
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape") document.body.classList.remove("nav-open");
        });
    }

    /* Wrap bare status words in table cells as coloured badges. */
    var STATUS = /^(PENDING|PAID|COMPLETED|CANCELLED|CANCELED|SCHEDULED|BOOKED|CONFIRMED|ACTIVE|INACTIVE|SUCCESS|FAILED|EXPIRED|AVAILABLE|PRESENT|ABSENT|NOT_AVAILABLE|LEAVE|DISPENSED|PARTIALLY_DISPENSED|LOW_STOCK|OUT_OF_STOCK|IN_STOCK|APPROVED)$/i;
    function badgeCells(scope) {
        (scope || document).querySelectorAll("td").forEach(function (td) {
            if (td.children.length) return;
            var t = (td.textContent || "").trim();
            if (!STATUS.test(t)) return;
            var span = document.createElement("span");
            span.className = "badge " + t.toLowerCase().replace(/canceled/, "cancelled");
            span.textContent = t.replace(/_/g, " ");
            td.textContent = "";
            td.appendChild(span);
        });
    }

    function observe() {
        if (!window.MutationObserver) return;
        var timer = null;
        new MutationObserver(function () {
            clearTimeout(timer);
            timer = setTimeout(function () {
                badgeCells();
                decorateCards();
            }, 40);
        }).observe(document.body, { childList: true, subtree: true });
    }

    function init() {
        document.querySelectorAll(".role-sidebar").forEach(function (s) { buildBrand(s); buildFooter(s); });
        decorateNav(document);
        decorateCards();
        groupFields();
        topbarAvatar();
        mobileMenu();
        badgeCells();
        observe();
        document.querySelectorAll("[data-icon]").forEach(function (el) {
            el.innerHTML = icon(el.getAttribute("data-icon"));
        });
        document.body.classList.add("ui-ready");
    }

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
    else init();
})();
