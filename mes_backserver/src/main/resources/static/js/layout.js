(() => {
  const body = document.body;
  const toggleButtons = document.querySelectorAll('[data-sidebar-toggle]');
  const overlay = document.querySelector('.sidebar-overlay');

  const isDesktop = () => window.innerWidth >= 1025;

  // 초기 상태 설정: 데스크톱에서 이전에 접어두었다면 유지
  if (isDesktop() && localStorage.getItem('sidebar-collapsed') === 'true') {
    body.classList.add('sidebar-collapsed');
  }

  const toggleSidebar = () => {
    if (isDesktop()) {
      // 데스크톱: 접힘/펼침 토글
      const isCollapsed = body.classList.toggle('sidebar-collapsed');
      localStorage.setItem('sidebar-collapsed', isCollapsed);
      
      // 데스크톱에서는 모바일용 open 클래스 제거
      body.classList.remove('sidebar-open');
    } else {
      // 모바일: 열림/닫힘 토글
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

  // ESC 키로 모바일 사이드바 닫기
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && body.classList.contains('sidebar-open')) {
      closeSidebar();
    }
  });

  // 화면 크기 변경 시 상태 정리
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
