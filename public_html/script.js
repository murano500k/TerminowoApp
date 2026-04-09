(function () {
  'use strict';

  /* ──────────────────────────────────────────────
     Constants
     ────────────────────────────────────────────── */
  var STORAGE_KEY = 'terminowo-lang';
  var SUPPORTED_LANGS = ['en', 'pl', 'ua', 'ru'];
  var LANG_NAMES = { en: 'English', pl: 'Polski', ua: 'Українська', ru: 'Русский' };
  var DEFAULT_LANG = 'en';

  /* ──────────────────────────────────────────────
     1. i18n Engine
     ────────────────────────────────────────────── */
  var translationCache = {};

  function detectLanguage() {
    var saved = localStorage.getItem(STORAGE_KEY);
    if (saved && SUPPORTED_LANGS.indexOf(saved) !== -1) return saved;

    var nav = (navigator.language || '').toLowerCase();
    for (var i = 0; i < SUPPORTED_LANGS.length; i++) {
      if (nav === SUPPORTED_LANGS[i] || nav.indexOf(SUPPORTED_LANGS[i] + '-') === 0) {
        return SUPPORTED_LANGS[i];
      }
    }
    // Special case: "uk" browser code maps to our "ua" key
    if (nav === 'uk' || nav.indexOf('uk-') === 0) return 'ua';

    return DEFAULT_LANG;
  }

  function loadTranslations(lang) {
    if (translationCache[lang]) {
      return Promise.resolve(translationCache[lang]);
    }
    return fetch('/i18n/' + lang + '.json')
      .then(function (res) { return res.json(); })
      .then(function (data) {
        translationCache[lang] = data;
        return data;
      });
  }

  function applyTranslations(translations, lang) {
    // Update html lang attribute
    document.documentElement.lang = lang;

    // Update document title
    if (translations['meta.title']) {
      document.title = translations['meta.title'];
    }

    // Update meta description
    var metaDesc = document.querySelector('meta[name="description"]');
    if (metaDesc && translations['meta.description']) {
      metaDesc.setAttribute('content', translations['meta.description']);
    }

    // Update all elements with data-i18n attribute
    var elements = document.querySelectorAll('[data-i18n]');
    for (var i = 0; i < elements.length; i++) {
      var el = elements[i];
      var key = el.getAttribute('data-i18n');
      var value = translations[key];
      if (value === undefined) continue;

      var attr = el.getAttribute('data-i18n-attr');
      if (attr) {
        el.setAttribute(attr, value);
      } else {
        // Replace literal \n with actual newlines
        el.textContent = value.replace(/\\n/g, '\n');
      }
    }

    // Update language dropdown trigger label
    updateLangTriggerLabel(lang);
  }

  var currentLang = DEFAULT_LANG;

  function setLang(lang) {
    if (SUPPORTED_LANGS.indexOf(lang) === -1) return;
    currentLang = lang;
    localStorage.setItem(STORAGE_KEY, lang);
    loadTranslations(lang).then(function (t) {
      applyTranslations(t, lang);
    });
  }

  /* ──────────────────────────────────────────────
     2. Header Scroll Behavior
     ────────────────────────────────────────────── */
  function setupHeaderScroll() {
    var header = document.getElementById('siteHeader');
    if (!header) return;

    window.addEventListener('scroll', function () {
      if (window.scrollY > 80) {
        header.classList.add('header--scrolled');
      } else {
        header.classList.remove('header--scrolled');
      }
    }, { passive: true });
  }

  /* ──────────────────────────────────────────────
     3. Language Dropdown
     ────────────────────────────────────────────── */
  function updateLangTriggerLabel(lang) {
    var dropdown = document.getElementById('langDropdown');
    if (!dropdown) return;
    var trigger = dropdown.querySelector('.lang-dropdown__trigger');
    if (trigger) {
      trigger.textContent = LANG_NAMES[lang] || lang;
    }

    // Also update the footer button label
    var footerBtn = document.getElementById('footerLangBtn');
    if (footerBtn) {
      footerBtn.textContent = LANG_NAMES[lang] || lang;
    }

    // Mark the active option
    var options = dropdown.querySelectorAll('[data-lang]');
    for (var i = 0; i < options.length; i++) {
      options[i].classList.toggle('is-active', options[i].getAttribute('data-lang') === lang);
    }
  }

  function setupLangDropdown() {
    var dropdown = document.getElementById('langDropdown');
    if (!dropdown) return;
    var trigger = dropdown.querySelector('.lang-dropdown__trigger');

    // Toggle dropdown on trigger click
    if (trigger) {
      trigger.addEventListener('click', function (e) {
        e.stopPropagation();
        dropdown.classList.toggle('is-open');
      });
    }

    // Handle language option clicks
    var options = dropdown.querySelectorAll('[data-lang]');
    for (var i = 0; i < options.length; i++) {
      options[i].addEventListener('click', function (e) {
        e.stopPropagation();
        var lang = this.getAttribute('data-lang');
        setLang(lang);
        dropdown.classList.remove('is-open');
      });
    }

    // Close on outside click
    document.addEventListener('click', function () {
      dropdown.classList.remove('is-open');
    });

    // Footer language button — toggle the same dropdown
    var footerBtn = document.getElementById('footerLangBtn');
    if (footerBtn) {
      footerBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        dropdown.classList.toggle('is-open');
        // Scroll to top so the dropdown is visible
        window.scrollTo({ top: 0, behavior: 'smooth' });
      });
    }
  }

  /* ──────────────────────────────────────────────
     4. FAQ Accordion
     ────────────────────────────────────────────── */
  function setupFaqAccordion() {
    var items = document.querySelectorAll('details.faq__item');
    if (!items.length) return;

    for (var i = 0; i < items.length; i++) {
      items[i].addEventListener('toggle', function () {
        if (!this.open) return;
        // Close all other open details
        for (var j = 0; j < items.length; j++) {
          if (items[j] !== this && items[j].open) {
            items[j].open = false;
          }
        }
      });
    }
  }

  /* ──────────────────────────────────────────────
     5. Smooth Scroll for Anchor Links
     ────────────────────────────────────────────── */
  function setupSmoothScroll() {
    var links = document.querySelectorAll('a[href^="#"]');
    for (var i = 0; i < links.length; i++) {
      links[i].addEventListener('click', function (e) {
        var href = this.getAttribute('href');
        if (href === '#') return;
        var target = document.querySelector(href);
        if (target) {
          e.preventDefault();
          target.scrollIntoView({ behavior: 'smooth' });
        }
      });
    }
  }

  /* ──────────────────────────────────────────────
     Init
     ────────────────────────────────────────────── */
  document.addEventListener('DOMContentLoaded', function () {
    // Detect and apply language
    currentLang = detectLanguage();
    loadTranslations(currentLang).then(function (t) {
      applyTranslations(t, currentLang);
    });

    // Set up UI behaviors
    setupHeaderScroll();
    setupLangDropdown();
    setupFaqAccordion();
    setupSmoothScroll();
  });
})();
