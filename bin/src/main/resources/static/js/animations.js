(() => {
  const prefersReduced =
    window.matchMedia &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (prefersReduced) return;

  const hoverLiftSelectors = [
    ".card",
    ".surface",
    ".feature-card",
    ".game-card",
    ".widget-card",
    ".btn",
  ];

  hoverLiftSelectors.forEach((sel) => {
    document.querySelectorAll(sel).forEach((el) => el.classList.add("hover-lift"));
  });

  const revealSelectors = [
    ".container",
    ".section-header",
    ".card",
    ".surface",
    ".feature-card",
    ".game-card",
    ".widget-card",
    "footer",
  ];

  const targets = new Set();
  revealSelectors.forEach((sel) => {
    document.querySelectorAll(sel).forEach((el) => targets.add(el));
  });

  const items = Array.from(targets).filter((el) => {
    if (!el || !(el instanceof HTMLElement)) return false;
    // Skip hero because it already animates via slideshow
    if (el.closest(".hero")) return false;
    return true;
  });

  items.forEach((el) => el.classList.add("reveal"));

  const io = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          io.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.15, rootMargin: "0px 0px -10% 0px" }
  );

  items.forEach((el) => io.observe(el));
  
  // Navbar Scroll Logic
  const nav = document.querySelector("header .navbar");
  const topBar = document.querySelector("header .top-bar");
  if (nav) {
    window.addEventListener("scroll", () => {
      if (window.scrollY > 50) {
        nav.classList.add("scrolled");
        if (topBar) topBar.classList.add("is-hidden");
      } else {
        nav.classList.remove("scrolled");
        if (topBar) topBar.classList.remove("is-hidden");
      }
    });
  }
})();

