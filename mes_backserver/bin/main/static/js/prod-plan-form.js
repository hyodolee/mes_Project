document.addEventListener('DOMContentLoaded', () => {
    // --- 1. 공통 유틸리티 ---
    const setupSearch = (config) => {
        let debounceTimer;
        let selectedIndex = -1;

        // 실시간 검색
        const handleInput = () => {
            clearTimeout(debounceTimer);
            const query = config.searchInput.value.trim();
            
            if (query.length < 1) {
                hideDropdown();
                return;
            }

            debounceTimer = setTimeout(() => fetchRealtime(query), 300);
        };

        const fetchRealtime = async (query) => {
            try {
                const response = await fetch(`${config.apiUrl}?${config.searchParam}=${encodeURIComponent(query)}`);
                const result = await response.json();

                if (result.success) {
                    renderDropdown(result.data);
                }
            } catch (error) {
                console.error('Fetch error:', error);
            }
        };

        const renderDropdown = (items) => {
            config.dropdown.innerHTML = '';
            if (items.length === 0) {
                hideDropdown();
                return;
            }

            items.forEach((item, index) => {
                const div = document.createElement('div');
                div.className = 'dropdown-item';
                div.innerHTML = config.renderDropdownItem(item);
                div.addEventListener('click', () => {
                    config.onSelect(item);
                    hideDropdown();
                });
                config.dropdown.appendChild(div);
            });

            showDropdown();
            selectedIndex = -1;
        };

        const showDropdown = () => config.dropdown.classList.add('is-active');
        const hideDropdown = () => config.dropdown.classList.remove('is-active');

        // 키보드 네비게이션
        config.searchInput.addEventListener('keydown', (e) => {
            const items = config.dropdown.querySelectorAll('.dropdown-item');
            if (!config.dropdown.classList.contains('is-active')) return;

            if (e.key === 'ArrowDown') {
                e.preventDefault();
                selectedIndex = Math.min(selectedIndex + 1, items.length - 1);
                updateSelection(items);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                selectedIndex = Math.max(selectedIndex - 1, 0);
                updateSelection(items);
            } else if (e.key === 'Enter') {
                e.preventDefault();
                if (selectedIndex > -1) items[selectedIndex].click();
            } else if (e.key === 'Escape') {
                hideDropdown();
            }
        });

        const updateSelection = (items) => {
            items.forEach((item, idx) => {
                item.classList.toggle('is-selected', idx === selectedIndex);
                if (idx === selectedIndex) item.scrollIntoView({ block: 'nearest' });
            });
        };

        // 모달 검색
        const openModal = () => {
            config.modal.classList.add('is-active');
            config.modalInput.value = config.searchInput.value;
            config.modalInput.focus();
            if (config.modalInput.value) searchModal();
        };

        const searchModal = async () => {
            const query = config.modalInput.value.trim();
            try {
                const response = await fetch(`${config.apiUrl}?${config.searchParam}=${encodeURIComponent(query)}`);
                const result = await response.json();

                if (result.success) {
                    renderModalList(result.data);
                }
            } catch (error) {
                alert('검색 중 오류가 발생했습니다.');
            }
        };

        const renderModalList = (items) => {
            config.modalTbody.innerHTML = '';
            if (items.length === 0) {
                config.modalTbody.innerHTML = `<tr><td colspan="${config.modalColSpan}" style="text-align:center; padding:20px;">결과가 없습니다.</td></tr>`;
                return;
            }

            items.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = config.renderModalRow(item);
                tr.querySelector('.btn-select').addEventListener('click', () => {
                    config.onSelect(item);
                    config.modal.classList.remove('is-active');
                });
                config.modalTbody.appendChild(tr);
            });
        };

        // 이벤트 바인딩
        config.searchInput.addEventListener('input', handleInput);
        config.btnSearch.addEventListener('click', openModal);
        config.modalBtnSearch.addEventListener('click', searchModal);
        config.modalInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                searchModal();
            }
        });
        
        config.modal.querySelector('.modal-close').addEventListener('click', () => config.modal.classList.remove('is-active'));
        
        document.addEventListener('click', (e) => {
            if (!config.searchInput.contains(e.target) && !config.dropdown.contains(e.target)) {
                hideDropdown();
            }
            if (e.target === config.modal) config.modal.classList.remove('is-active');
        });
    };

    // --- 2. 고객사 검색 설정 ---
    setupSearch({
        searchInput: document.getElementById('customerNm'),
        dropdown: document.getElementById('customerDropdown'),
        btnSearch: document.getElementById('btnCustomerSearch'),
        modal: document.getElementById('customerModal'),
        modalInput: document.getElementById('modalSearchInput'),
        modalBtnSearch: document.getElementById('btnDoModalSearch'),
        modalTbody: document.getElementById('customerListBody'),
        apiUrl: '/api/v1/master/companies',
        searchParam: 'companyNm',
        modalColSpan: 4,
        renderDropdownItem: (item) => `
            <span class="item-nm">${item.companyNm}</span>
            <span class="item-cd">${item.companyCd}</span>
        `,
        renderModalRow: (item) => `
            <td>${item.companyCd}</td>
            <td><strong>${item.companyNm}</strong></td>
            <td>${item.bizNo || '-'}</td>
            <td><button type="button" class="btn btn-primary btn-select">선택</button></td>
        `,
        onSelect: (item) => {
            document.getElementById('customerNm').value = item.companyNm;
            document.getElementById('customerCd').value = item.companyCd;
        }
    });

    // --- 3. 품목 검색 설정 ---
    setupSearch({
        searchInput: document.getElementById('itemSearch'),
        dropdown: document.getElementById('itemDropdown'),
        btnSearch: document.getElementById('btnItemSearch'),
        modal: document.getElementById('itemModal'),
        modalInput: document.getElementById('modalItemSearchInput'),
        modalBtnSearch: document.getElementById('btnDoItemModalSearch'),
        modalTbody: document.getElementById('itemListBody'),
        apiUrl: '/api/v1/master/items',
        searchParam: 'itemNm',
        modalColSpan: 4,
        renderDropdownItem: (item) => `
            <span class="item-nm">${item.itemNm}</span>
            <span class="item-cd">${item.itemCd} | ${item.itemType}</span>
        `,
        renderModalRow: (item) => `
            <td>${item.itemCd}</td>
            <td><strong>${item.itemNm}</strong></td>
            <td>${item.itemType || '-'}</td>
            <td><button type="button" class="btn btn-primary btn-select">선택</button></td>
        `,
        onSelect: (item) => {
            document.getElementById('itemSearch').value = item.itemNm; // 화면에는 품목명 표시
            document.getElementById('itemCd').value = item.itemCd; // 실제 전송되는 값
        }
    });

    // --- 4. 수주번호 검색 설정 ---
    setupSearch({
        searchInput: document.getElementById('orderSearch'),
        dropdown: document.getElementById('orderDropdown'),
        btnSearch: document.getElementById('btnOrderSearch'),
        modal: document.getElementById('orderModal'),
        modalInput: document.getElementById('modalOrderSearchInput'),
        modalBtnSearch: document.getElementById('btnDoOrderModalSearch'),
        modalTbody: document.getElementById('orderListBody'),
        apiUrl: '/api/v1/planning/orders/search',
        searchParam: 'keyword',
        modalColSpan: 5,
        renderDropdownItem: (item) => `
            <span class="item-nm">${item.orderNo} | ${item.customerNm}</span>
            <span class="item-cd">품목: ${item.itemNm || item.itemCd} | 수량: ${item.orderQty} | 납기: ${item.deliveryDt}</span>
        `,
        renderModalRow: (item) => `
            <td><strong>${item.orderNo}</strong></td>
            <td>${item.customerNm}</td>
            <td>${item.orderDt || '-'}</td>
            <td><span style="color:var(--primary-dark); font-weight:700;">${item.deliveryDt}</span></td>
            <td><button type="button" class="btn btn-primary btn-select">선택</button></td>
        `,
        onSelect: (item) => {
            document.getElementById('orderSearch').value = item.orderNo;
            document.getElementById('orderNo').value = item.orderNo;
            
            // 수주 정보 바탕으로 폼 자동 채우기
            if (item.customerNm) document.getElementById('customerNm').value = item.customerNm;
            if (item.customerCd) document.getElementById('customerCd').value = item.customerCd;
            if (item.itemNm) document.getElementById('itemSearch').value = item.itemNm;
            if (item.itemCd) document.getElementById('itemCd').value = item.itemCd;
            if (item.orderQty) document.getElementById('planQty').value = item.orderQty;
            if (item.deliveryDt) document.getElementById('deliveryDt').value = item.deliveryDt;
            
            // 시각적 피드백
            const el = document.getElementById('orderSearch');
            el.style.backgroundColor = '#f0fdfa';
            setTimeout(() => el.style.backgroundColor = '', 1000);
        }
    });
});