(() => {
  const body = document.body;
  const toggleButtons = document.querySelectorAll('[data-sidebar-toggle]');
  const overlay = document.querySelector('.sidebar-overlay');

  const isDesktop = () => window.innerWidth >= 1025;

  if (isDesktop() && localStorage.getItem('sidebar-collapsed') === 'true') {
    body.classList.add('sidebar-collapsed');
  }

  const toggleSidebar = () => {
    if (isDesktop()) {
      const isCollapsed = body.classList.toggle('sidebar-collapsed');
      localStorage.setItem('sidebar-collapsed', isCollapsed);
      body.classList.remove('sidebar-open');
    } else {
      body.classList.toggle('sidebar-open');
    }
  };

  const closeSidebar = () => {
    body.classList.remove('sidebar-open');
  };

  toggleButtons.forEach((button) => {
    button.addEventListener('click', (e) => {
      e.preventDefault();
      toggleSidebar();
    });
  });

  if (overlay) {
    overlay.addEventListener('click', closeSidebar);
  }

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && body.classList.contains('sidebar-open')) {
      closeSidebar();
    }
  });

  let resizeTimer;
  window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      if (isDesktop()) {
        closeSidebar();
      }
    }, 250);
  });
})();
