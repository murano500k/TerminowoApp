// Language toggle (PL ↔ EN)
(function () {
    var STORAGE_KEY = 'terminowo-lang';
    var html = document.documentElement;
    var btn = document.getElementById('langToggle');

    // Restore saved language or default to PL
    var saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'en' || saved === 'pl') {
        html.setAttribute('lang', saved);
    }

    btn.addEventListener('click', function () {
        var current = html.getAttribute('lang') || 'pl';
        var next = current === 'pl' ? 'en' : 'pl';
        html.setAttribute('lang', next);
        localStorage.setItem(STORAGE_KEY, next);
    });
})();
