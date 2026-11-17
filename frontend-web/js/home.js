async function loadStats() {
    const el = document.getElementById('catalog-stats');
    try {
        const s = await PythonApi.stats();
        const maxArea = Math.max(1, ...s.byArea.map((a) => a.count));
        const maxLevel = Math.max(1, ...s.byDifficulty.map((d) => d.count));

        el.innerHTML = `
            <div class="stats-kpis">
                <div class="kpi"><span class="kpi-value">${s.totalProblems}</span><span class="kpi-label">problemas</span></div>
                <div class="kpi"><span class="kpi-value">${s.byArea.length}</span><span class="kpi-label">áreas com problemas</span></div>
                <div class="kpi"><span class="kpi-value">${s.totalRatings}</span><span class="kpi-label">avaliações de dificuldade</span></div>
                <div class="kpi"><span class="kpi-value">${s.averageDifficulty !== null ? formatDecimal(s.averageDifficulty) : '—'}</span><span class="kpi-label">dificuldade média (1–5)</span></div>
            </div>
            <div class="stats-grid">
                <div>
                    <div class="stats-subtitle">Por área</div>
                    <div class="bar-chart">
                        ${s.byArea.map((a) => `
                            <a class="bar-row" href="/pages/catalog.html?area=${encodeURIComponent(a.area)}">
                                <span class="bar-label">${escapeHtml(a.area)}</span>
                                <span class="bar-track"><span class="bar-fill" style="width:${(a.count / maxArea) * 100}%"></span></span>
                                <span class="bar-value">${a.count}</span>
                            </a>`).join('')}
                    </div>
                </div>
                <div>
                    <div class="stats-subtitle">Por dificuldade</div>
                    <div class="bar-chart">
                        ${s.byDifficulty.map((d) => `
                            <a class="bar-row" href="/pages/catalog.html?minDifficulty=${d.level}&maxDifficulty=${d.level}">
                                <span class="bar-label">${d.level} · ${difficultyLabel(d.level)}</span>
                                <span class="bar-track"><span class="bar-fill" style="width:${(d.count / maxLevel) * 100}%"></span></span>
                                <span class="bar-value">${d.count}</span>
                            </a>`).join('')}
                    </div>
                    ${s.topTags.length ? `
                        <div class="stats-subtitle mt-5">Tópicos mais frequentes</div>
                        <div class="tag-list">
                            ${s.topTags.map((t) => `<a class="tag tag-link" href="/pages/catalog.html?tag=${encodeURIComponent(t.tag)}">${escapeHtml(t.tag)} · ${t.count}</a>`).join('')}
                        </div>` : ''}
                </div>
            </div>`;
    } catch (err) {
        renderErrorAlert(el, err.message);
    }
}

async function loadAreaIndex() {
    const el = document.getElementById('area-index');
    try {
        const areas = await JavaApi.listAreas();
        if (areas.length === 0) {
            renderEmpty(el, 'Nenhuma área ainda', 'As áreas aparecem aqui assim que forem cadastradas.');
            return;
        }
        const counts = await Promise.all(
            areas.map((a) => JavaApi.searchProblems({ area: a.name, size: 1 }).then((p) => p.totalElements).catch(() => 0))
        );
        el.innerHTML = areas
            .map((a, i) => `
                <a class="index-row" href="/pages/catalog.html?area=${encodeURIComponent(a.name)}">
                    <span class="index-name">${escapeHtml(a.name)}</span>
                    <span class="index-count">${plural(counts[i], 'problema')}</span>
                </a>`)
            .join('');
    } catch (err) {
        renderErrorAlert(el, err.message);
    }
}

async function loadRecentProblems() {
    const el = document.getElementById('recent-problems');
    try {
        const page = await JavaApi.searchProblems({ size: 6 });
        if (page.content.length === 0) {
            renderEmpty(el, 'Nenhum problema ainda', 'Os problemas aparecem aqui assim que forem cadastrados.');
            return;
        }
        el.innerHTML = problemListHtml(page.content);
        typesetMath(el);
    } catch (err) {
        renderErrorAlert(el, err.message);
    }
}

function renderDifficultyExplore() {
    const el = document.getElementById('difficulty-explore');
    el.innerHTML = [1, 2, 3, 4, 5].map((lvl) => `
        <a class="difficulty-explore-row" href="/pages/catalog.html?minDifficulty=${lvl}&maxDifficulty=${lvl}">
            <span class="level-num">0${lvl}</span>
            <span class="level-name">${difficultyLabel(lvl)}</span>
            ${difficultyBarsHtml(lvl)}
        </a>`).join('');
}

document.getElementById('home-search-form').addEventListener('submit', (evt) => {
    evt.preventDefault();
    const text = document.getElementById('home-search-input').value.trim();
    window.location.href = `/pages/catalog.html${text ? `?text=${encodeURIComponent(text)}` : ''}`;
});

document.getElementById('random-btn').addEventListener('click', async (evt) => {
    const btn = evt.currentTarget;
    try {
        await withButtonLoading(btn, 'Sorteando…', () => goToRandomProblem());
    } catch (err) {
        showToast(err.message, 'error');
    }
});
loadStats();
loadAreaIndex();
loadRecentProblems();
renderDifficultyExplore();
