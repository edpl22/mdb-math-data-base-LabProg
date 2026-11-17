async function goToRandomProblem(params = {}) {
    const problem = await PythonApi.randomProblem(params);
    window.location.href = `/pages/problem.html?id=${problem.id}`;
}

function renderHeader() {
    const mount = document.getElementById('app-header');
    if (!mount) return;

    const active = window.MDB_ACTIVE_PAGE || '';
    const loggedIn = Auth.isLoggedIn();
    const isAdmin = Auth.isAdmin();
    const user = Auth.getUser();

    const link = (href, label, key) =>
        `<a href="${href}" class="${active === key ? 'active' : ''}" ${active === key ? 'aria-current="page"' : ''}>${label}</a>`;

    const navLinks = [
        link('/pages/catalog.html', 'Problemas', 'catalog'),
        link('/pages/mock-exam.html', 'Simulado', 'mock-exam'),
        `<a href="#" data-random-link>Sortear</a>`,
    ];
    if (loggedIn) navLinks.push(link('/pages/profile.html', 'Meu perfil', 'profile'));
    if (isAdmin) navLinks.push(link('/pages/admin.html', 'Admin', 'admin'));

    const firstName = user && user.name ? user.name.split(' ')[0] : '';
    const userArea = loggedIn
        ? `<div class="nav-user">
                <span class="user-name">${escapeHtml(firstName)}${isAdmin ? '<span class="role-tag">ADMIN</span>' : ''}</span>
                <button class="btn btn-secondary btn-sm" id="logout-btn">Sair</button>
           </div>`
        : `<div class="nav-user">
                <a href="/pages/login.html" class="nav-link-plain">Entrar</a>
                <a href="/pages/register.html" class="btn btn-sm">Criar conta</a>
           </div>`;

    mount.innerHTML = `
        <div class="container">
            <button class="nav-burger" id="nav-burger" type="button" aria-label="Abrir menu" aria-expanded="false" aria-controls="mobile-nav">
                <span></span><span></span><span></span>
            </button>
            <a class="brand" href="/index.html" title="MDB - Math Data Base">MDB<span class="brand-sub">math data base</span></a>
            <nav class="main-nav" aria-label="Principal">${navLinks.join('')}</nav>
            <div class="nav-search">
                <form id="nav-search-form" class="nav-search-input" role="search">
                    <svg class="search-icon" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                        <circle cx="7" cy="7" r="5.25"/><path d="M11 11L14.5 14.5"/>
                    </svg>
                    <input type="search" id="nav-search-input" placeholder="Buscar problemas, áreas, tópicos..." autocomplete="off" aria-label="Buscar problemas" />
                    <span class="kbd-hint" id="nav-search-kbd">Ctrl K</span>
                </form>
            </div>
            ${userArea}
        </div>
        <nav class="mobile-nav" id="mobile-nav" aria-label="Menu" hidden>
            <div class="container">${navLinks.join('')}</div>
        </nav>`;

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            Auth.clearSession();
            showToast('Você saiu da sua conta.', 'info');
            setTimeout(() => { window.location.href = '/index.html'; }, 350);
        });
    }

    const burger = document.getElementById('nav-burger');
    const mobileNav = document.getElementById('mobile-nav');
    burger.addEventListener('click', () => {
        const open = mobileNav.hidden;
        mobileNav.hidden = !open;
        burger.setAttribute('aria-expanded', String(open));
        burger.classList.toggle('open', open);
    });

    mount.querySelectorAll('[data-random-link]').forEach((randomLink) => {
        randomLink.addEventListener('click', async (evt) => {
            evt.preventDefault();
            const original = randomLink.textContent;
            randomLink.textContent = 'Sorteando…';
            try {
                await goToRandomProblem();
            } catch (err) {
                showToast(err.message, 'error');
                randomLink.textContent = original;
            }
        });
    });

    const searchForm = document.getElementById('nav-search-form');
    const searchInput = document.getElementById('nav-search-input');
    searchForm.addEventListener('submit', (evt) => {
        evt.preventDefault();
        const text = searchInput.value.trim();
        window.location.href = `/pages/catalog.html${text ? `?text=${encodeURIComponent(text)}` : ''}`;
    });

    document.addEventListener('keydown', (evt) => {
        if ((evt.ctrlKey || evt.metaKey) && evt.key.toLowerCase() === 'k') {
            evt.preventDefault();
            searchInput.focus();
            searchInput.select();
        }
    });
    const kbd = document.getElementById('nav-search-kbd');
    searchInput.addEventListener('focus', () => { kbd.style.visibility = 'hidden'; });
    searchInput.addEventListener('blur', () => { kbd.style.visibility = 'visible'; });
}

function renderFooter() {
    const mount = document.getElementById('app-footer');
    if (!mount) return;
    mount.innerHTML = `
        <div class="container footer-inner">
            <span>MDB - Math Data Base · Projeto Final · Laboratório de Programação II</span>
        </div>`;
}

document.addEventListener('DOMContentLoaded', () => {
    renderHeader();
    renderFooter();
});
