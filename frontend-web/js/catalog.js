const PAGE_SIZE = 20;

const state = {
    text: '',
    area: '',
    tag: '',
    minDifficulty: 1,
    maxDifficulty: 5,
    page: 0,
};

let allAreas = [];
let allTags = [];

function clampLevel(value, fallback) {
    const n = parseInt(value, 10);
    return Number.isFinite(n) && n >= 1 && n <= 5 ? n : fallback;
}

function readStateFromUrl() {
    const params = new URLSearchParams(window.location.search);
    state.text = params.get('text') || '';
    state.area = params.get('area') || '';
    state.tag = params.get('tag') || '';
    state.minDifficulty = clampLevel(params.get('minDifficulty'), 1);
    state.maxDifficulty = clampLevel(params.get('maxDifficulty'), 5);
    if (state.maxDifficulty < state.minDifficulty) state.maxDifficulty = state.minDifficulty;
    state.page = Math.max(0, parseInt(params.get('page') || '0', 10) || 0);
}

function writeStateToUrl() {
    const params = new URLSearchParams();
    if (state.text) params.set('text', state.text);
    if (state.area) params.set('area', state.area);
    if (state.tag) params.set('tag', state.tag);
    if (state.minDifficulty !== 1) params.set('minDifficulty', state.minDifficulty);
    if (state.maxDifficulty !== 5) params.set('maxDifficulty', state.maxDifficulty);
    if (state.page !== 0) params.set('page', state.page);
    const qs = params.toString();
    history.replaceState(null, '', `${window.location.pathname}${qs ? '?' + qs : ''}`);
}

function renderAreaOptions() {
    const el = document.getElementById('filter-area-options');
    el.innerHTML = allAreas.map((a) => `
        <button type="button" class="filter-option ${state.area === a.name ? 'checked' : ''}" data-area="${escapeHtml(a.name)}" aria-pressed="${state.area === a.name}">
            <span class="box"></span><span>${escapeHtml(a.name)}</span>
        </button>`).join('');
    el.querySelectorAll('.filter-option').forEach((btn) => {
        btn.addEventListener('click', () => {
            state.area = state.area === btn.dataset.area ? '' : btn.dataset.area;
            state.page = 0;
            renderAreaOptions();
            runSearch();
        });
    });
}

function renderTopicOptions(filterText = '') {
    const el = document.getElementById('filter-topic-options');
    const normalized = filterText.trim().toLowerCase();
    const filtered = normalized
        ? allTags.filter((t) => t.name.toLowerCase().includes(normalized))
        : allTags;

    if (filtered.length === 0) {
        el.innerHTML = `<p class="hint-text">Nenhum tópico encontrado.</p>`;
        return;
    }
    el.innerHTML = filtered.slice(0, 30).map((t) => `
        <button type="button" class="filter-option ${state.tag === t.name ? 'checked' : ''}" data-tag="${escapeHtml(t.name)}" aria-pressed="${state.tag === t.name}">
            <span class="box"></span><span>${escapeHtml(t.name)}</span>
        </button>`).join('');
    el.querySelectorAll('.filter-option').forEach((btn) => {
        btn.addEventListener('click', () => {
            state.tag = state.tag === btn.dataset.tag ? '' : btn.dataset.tag;
            state.page = 0;
            renderTopicOptions(document.getElementById('filter-topic-search').value);
            runSearch();
        });
    });
}

function renderDifficultyFilters() {
    renderDifficultySelector('filter-min-difficulty', state.minDifficulty, (v) => {
        state.minDifficulty = v;
        if (state.maxDifficulty < v) state.maxDifficulty = v;
        state.page = 0;
        renderDifficultyFilters();
        runSearch();
    });
    renderDifficultySelector('filter-max-difficulty', state.maxDifficulty, (v) => {
        state.maxDifficulty = v;
        if (state.minDifficulty > v) state.minDifficulty = v;
        state.page = 0;
        renderDifficultyFilters();
        runSearch();
    });
}

function renderActiveFilters() {
    const el = document.getElementById('active-filters');
    const chips = [];
    if (state.text) chips.push({ key: 'text', label: `“${state.text}”` });
    if (state.area) chips.push({ key: 'area', label: state.area });
    if (state.tag) chips.push({ key: 'tag', label: `#${state.tag}` });
    if (state.minDifficulty !== 1 || state.maxDifficulty !== 5) {
        chips.push({ key: 'difficulty', label: `Dificuldade ${state.minDifficulty}–${state.maxDifficulty}` });
    }
    el.innerHTML = chips.map((c) =>
        `<button type="button" class="chip" data-chip="${c.key}" title="Remover filtro">${escapeHtml(c.label)} <span aria-hidden="true">×</span></button>`
    ).join('');
    el.querySelectorAll('[data-chip]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const key = btn.dataset.chip;
            if (key === 'text') { state.text = ''; document.getElementById('f-text').value = ''; }
            if (key === 'area') { state.area = ''; renderAreaOptions(); }
            if (key === 'tag') { state.tag = ''; renderTopicOptions(document.getElementById('filter-topic-search').value); }
            if (key === 'difficulty') { state.minDifficulty = 1; state.maxDifficulty = 5; renderDifficultyFilters(); }
            state.page = 0;
            runSearch();
        });
    });
}

async function loadFilterOptions() {
    try {
        [allAreas, allTags] = await Promise.all([JavaApi.listAreas(), JavaApi.listTags()]);
        renderAreaOptions();
        renderTopicOptions();
    } catch (err) {
        showToast('Não foi possível carregar áreas/tópicos: ' + err.message, 'error');
    }
}

function renderPagination(page) {
    const el = document.getElementById('pagination');
    if (page.totalPages <= 1) { el.innerHTML = ''; return; }
    el.innerHTML = `
        <button id="prev-page" ${page.page === 0 ? 'disabled' : ''}>← Anterior</button>
        <span class="page-info">Página ${page.page + 1} de ${page.totalPages}</span>
        <button id="next-page" ${page.page >= page.totalPages - 1 ? 'disabled' : ''}>Próxima →</button>
    `;
    const go = (delta) => { state.page += delta; runSearch(); window.scrollTo({ top: 0, behavior: 'smooth' }); };
    document.getElementById('prev-page')?.addEventListener('click', () => go(-1));
    document.getElementById('next-page')?.addEventListener('click', () => go(1));
}

let searchSeq = 0;

async function runSearch() {
    writeStateToUrl();
    renderActiveFilters();
    const seq = ++searchSeq;
    const container = document.getElementById('problem-list-container');
    const info = document.getElementById('results-info');
    renderLoading(container, 'Buscando…');
    try {
        const result = await JavaApi.searchProblems({
            text: state.text || undefined,
            area: state.area || undefined,
            tag: state.tag || undefined,
            minDifficulty: state.minDifficulty,
            maxDifficulty: state.maxDifficulty,
            page: state.page,
            size: PAGE_SIZE,
        });
        if (seq !== searchSeq) return; // resposta de uma busca antiga: descarta

        if (result.content.length === 0) {
            renderEmpty(container, 'Nenhum problema encontrado', 'Tente ajustar os filtros ou o texto da busca.',
                '<button class="btn btn-secondary btn-sm mt-3" type="button" onclick="document.getElementById(\'clear-filters\').click()">Limpar filtros</button>');
            info.textContent = '0 resultados';
        } else {
            container.innerHTML = problemListHtml(result.content);
            typesetMath(container);
            info.textContent = `${plural(result.totalElements, 'problema encontrado', 'problemas encontrados')}`;
        }
        renderPagination(result);
    } catch (err) {
        if (seq !== searchSeq) return;
        renderErrorAlert(container, err.message);
        info.textContent = '';
    }
}

let textDebounce;
const textInput = document.getElementById('f-text');
textInput.addEventListener('keydown', (evt) => {
    if (evt.key === 'Enter') {
        evt.preventDefault();
        clearTimeout(textDebounce);
        state.text = evt.target.value.trim();
        state.page = 0;
        runSearch();
    }
});
textInput.addEventListener('input', (evt) => {
    clearTimeout(textDebounce);
    textDebounce = setTimeout(() => {
        const value = evt.target.value.trim();
        if (value === state.text) return;
        state.text = value;
        state.page = 0;
        runSearch();
    }, 450);
});

document.getElementById('filter-topic-search').addEventListener('input', (evt) => {
    renderTopicOptions(evt.target.value);
});

document.getElementById('clear-filters').addEventListener('click', () => {
    state.text = ''; state.area = ''; state.tag = ''; state.minDifficulty = 1; state.maxDifficulty = 5; state.page = 0;
    document.getElementById('f-text').value = '';
    document.getElementById('filter-topic-search').value = '';
    renderAreaOptions();
    renderTopicOptions();
    renderDifficultyFilters();
    runSearch();
});

document.getElementById('filters-toggle').addEventListener('click', (evt) => {
    const sidebar = document.getElementById('filters-sidebar');
    const open = sidebar.classList.toggle('open');
    evt.currentTarget.setAttribute('aria-expanded', String(open));
    document.getElementById('filters-toggle-icon').textContent = open ? '−' : '+';
});

document.getElementById('random-filtered-btn').addEventListener('click', async (evt) => {
    try {
        await withButtonLoading(evt.currentTarget, 'Sorteando…', () => goToRandomProblem({
            area: state.area || undefined,
            tag: state.tag || undefined,
            minDifficulty: state.minDifficulty,
            maxDifficulty: state.maxDifficulty,
        }));
    } catch (err) {
        showToast(err.message, 'error');
    }
});

(async function init() {
    readStateFromUrl();
    document.getElementById('f-text').value = state.text;
    renderDifficultyFilters();
    await loadFilterOptions();
    runSearch();
})();
