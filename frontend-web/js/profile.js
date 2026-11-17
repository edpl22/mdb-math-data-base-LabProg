(function () {
    if (!Auth.isLoggedIn()) {
        window.location.href = `/pages/login.html?next=${encodeURIComponent('/pages/profile.html')}`;
        return;
    }

    document.querySelectorAll('.tab-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach((b) => {
                b.classList.toggle('active', b === btn);
                b.setAttribute('aria-selected', String(b === btn));
            });
            document.querySelectorAll('.tab-panel').forEach((p) => p.classList.remove('active'));
            document.getElementById(`panel-${btn.dataset.tab}`).classList.add('active');
        });
    });

    const browseBtn = '<a class="btn btn-secondary btn-sm mt-3" href="/pages/catalog.html">Explorar o catálogo</a>';

    function renderSection(key, problems, emptyTitle, emptyMessage) {
        const el = document.getElementById(`grid-${key}`);
        document.getElementById(`count-${key}`).textContent = (problems || []).length;
        if (!problems || problems.length === 0) {
            renderEmpty(el, emptyTitle, emptyMessage, browseBtn);
            return;
        }
        el.innerHTML = problemListHtml(problems);
        typesetMath(el);
    }

    async function loadProfile() {
        const user = Auth.getUser();
        document.getElementById('profile-title').textContent = user.name;
        document.getElementById('profile-sub').textContent =
            `${user.email}${user.role === 'ADMIN' ? ' · administrador' : ''}`;

        try {
            const data = await JavaApi.myProblems();
            renderSection('favorites', data.favorites, 'Nenhum favorito ainda', 'Marque problemas como favoritos para encontrá-los aqui.');
            renderSection('wantToDo', data.wantToDo, 'Nada em “quero resolver”', 'Marque problemas que você pretende resolver depois.');
            renderSection('done', data.done, 'Nenhum problema resolvido ainda', 'Marque problemas como resolvidos conforme avança.');

            document.getElementById('profile-kpis').innerHTML = `
                <div class="kpi"><span class="kpi-value">${data.favorites.length}</span><span class="kpi-label">favoritos</span></div>
                <div class="kpi"><span class="kpi-value">${data.wantToDo.length}</span><span class="kpi-label">quero resolver</span></div>
                <div class="kpi"><span class="kpi-value">${data.done.length}</span><span class="kpi-label">resolvidos</span></div>`;
        } catch (err) {
            renderErrorAlert(document.getElementById('grid-favorites'), err.message);
        }
    }

    loadProfile();
})();
