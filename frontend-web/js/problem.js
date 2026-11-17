function getProblemId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

const STATUS_LABELS = {
    FAVORITE: '★ Favorito',
    WANT_TO_DO: 'Quero resolver',
    DONE: '✓ Resolvido',
};

function statusButton(status, activeStatuses) {
    const active = activeStatuses.includes(status);
    return `<button class="btn btn-toggle ${active ? 'is-active' : ''}" aria-pressed="${active}"
                data-status="${status}" data-active="${active}">${STATUS_LABELS[status]}</button>`;
}

function formatDate(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' });
}

function renderProblem(problem) {
    const content = document.getElementById('page-content');
    const loggedIn = Auth.isLoggedIn();
    const statuses = problem.userStatuses || [];
    document.title = `${problem.title} | MDB`;

    const tags = (problem.tags || [])
        .map((t) => `<a class="tag tag-link" href="/pages/catalog.html?tag=${encodeURIComponent(t)}">${escapeHtml(t)}</a>`)
        .join('');

    content.innerHTML = `
        <div class="flex-between problem-topbar">
            <div class="breadcrumb">
                <a href="/pages/catalog.html">Problemas</a> /
                <a href="/pages/catalog.html?area=${encodeURIComponent(problem.area)}">${escapeHtml(problem.area || '')}</a>
            </div>
        </div>

        <div class="problem-header">
            <div class="problem-id">Problema #${problem.id}</div>
            <h1>${escapeHtml(problem.title)}</h1>
            <div class="tag-list">${tags}</div>
        </div>

        <div class="metadata-row">
            <div class="metadata-item">
                <span class="m-label">Dificuldade comunitária</span>
                <span class="m-value">${problem.communityDifficulty !== null && problem.communityDifficulty !== undefined ? formatDecimal(problem.communityDifficulty) + ' / 5' : 'Sem avaliações'}</span>
            </div>
            <div class="metadata-item">
                <span class="m-label">Dificuldade inicial</span>
                <span class="m-value">${problem.initialDifficulty} / 5 · ${difficultyLabel(problem.initialDifficulty)}</span>
            </div>
            <div class="metadata-item">
                <span class="m-label">Avaliações</span>
                <span class="m-value">${problem.ratingsCount}</span>
            </div>
            <div class="metadata-item">
                <span class="m-label">Fonte</span>
                <span class="m-value">${escapeHtml(problem.source || '—')}</span>
            </div>
            <div class="metadata-item">
                <span class="m-label">Cadastrado por</span>
                <span class="m-value">${escapeHtml(problem.createdByName || '—')}</span>
            </div>
            <div class="metadata-item">
                <span class="m-label">Adicionado em</span>
                <span class="m-value">${formatDate(problem.createdAt)}</span>
            </div>
        </div>

        <div class="problem-actions-row" id="status-actions">
            ${loggedIn
                ? ['FAVORITE', 'WANT_TO_DO', 'DONE'].map((s) => statusButton(s, statuses)).join('')
                : `<a class="btn btn-secondary" href="/pages/login.html?next=${encodeURIComponent(location.pathname + location.search)}">Entre para favoritar ou acompanhar seu progresso</a>`}
            <span class="actions-spacer"></span>
            <button class="btn btn-secondary" id="random-same-area" type="button" title="Sorteia outro problema da mesma área">Sortear outro de ${escapeHtml(problem.area)}</button>
        </div>

        <div class="container-read">
            <div class="reveal-section">
                <div class="rs-header"><span class="rs-title">Enunciado</span></div>
                <div class="statement-block">${escapeHtml(problem.statement)}</div>
            </div>

            <div class="reveal-section">
                <div class="rs-header">
                    <span class="rs-title">Dica</span>
                    <button class="btn btn-secondary btn-sm" id="toggle-hint" aria-expanded="false" aria-controls="hint-content">Mostrar dica</button>
                </div>
                <div class="reveal-content hidden" id="hint-content">${escapeHtml(problem.hint)}</div>
            </div>

            <div class="reveal-section">
                <div class="rs-header">
                    <span class="rs-title">Solução</span>
                    <button class="btn btn-secondary btn-sm" id="toggle-solution" aria-expanded="false" aria-controls="solution-content">Mostrar solução</button>
                </div>
                <div class="reveal-content hidden" id="solution-content">${escapeHtml(problem.solution)}</div>
            </div>

            <div class="difficulty-panel">
                <span class="rs-title">Quão difícil foi para você?</span>
                <div class="dp-current mt-3">
                    ${difficultyFullHtml(problem.communityDifficulty, problem.ratingsCount)}
                </div>
                ${loggedIn ? `
                    <div class="difficulty-select" id="difficulty-scale" role="group" aria-label="Sua avaliação de dificuldade">
                        ${[1, 2, 3, 4, 5].map((v) => `<button type="button" data-value="${v}" title="${v} · ${difficultyLabel(v)}" class="${problem.userDifficulty === v ? 'selected' : ''}">${v}</button>`).join('')}
                    </div>
                    <p class="hint-text">${problem.userDifficulty
                        ? `Sua avaliação: <strong>${problem.userDifficulty} · ${difficultyLabel(problem.userDifficulty)}</strong>. Clique em outro valor para alterar.`
                        : '1 = fundamental · 5 = muito avançado. Você pode mudar depois.'}</p>
                ` : `<p><a href="/pages/login.html?next=${encodeURIComponent(location.pathname + location.search)}">Entre</a> para avaliar a dificuldade deste problema.</p>`}
            </div>
        </div>
    `;

    typesetMath(content);

    const bindToggle = (btnId, boxId, showLabel, hideLabel) => {
        document.getElementById(btnId).addEventListener('click', (evt) => {
            const box = document.getElementById(boxId);
            const hidden = box.classList.toggle('hidden');
            evt.currentTarget.textContent = hidden ? showLabel : hideLabel;
            evt.currentTarget.setAttribute('aria-expanded', String(!hidden));
            if (!hidden) typesetMath(box);
        });
    };
    bindToggle('toggle-hint', 'hint-content', 'Mostrar dica', 'Ocultar dica');
    bindToggle('toggle-solution', 'solution-content', 'Mostrar solução', 'Ocultar solução');

    document.getElementById('random-same-area').addEventListener('click', async (evt) => {
        try {
            await withButtonLoading(evt.currentTarget, 'Sorteando…', async () => {
                // evita sortear o mesmo problema
                for (let i = 0; i < 4; i++) {
                    const next = await PythonApi.randomProblem({ area: problem.area });
                    if (next.id !== problem.id || i === 3) {
                        window.location.href = `/pages/problem.html?id=${next.id}`;
                        return;
                    }
                }
            });
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    if (loggedIn) {
        document.querySelectorAll('[data-status]').forEach((btn) => {
            btn.addEventListener('click', () => handleStatusToggle(problem.id, btn));
        });
        document.querySelectorAll('#difficulty-scale button').forEach((btn) => {
            btn.addEventListener('click', () => handleDifficultyRate(problem, parseInt(btn.dataset.value, 10)));
        });
    }
}

async function reloadProblem(id) {
    const updated = await JavaApi.getProblem(id);
    const hintOpen = !document.getElementById('hint-content')?.classList.contains('hidden');
    const solutionOpen = !document.getElementById('solution-content')?.classList.contains('hidden');
    renderProblem(updated);
    if (hintOpen) document.getElementById('toggle-hint').click();
    if (solutionOpen) document.getElementById('toggle-solution').click();
}

async function handleStatusToggle(problemId, btn) {
    const status = btn.dataset.status;
    const isActive = btn.dataset.active === 'true';
    btn.disabled = true;
    try {
        if (isActive) {
            await JavaApi.removeStatus(problemId, status);
            showToast(`Removido de “${STATUS_LABELS[status].replace(/^[★✓] /, '')}”.`, 'info');
        } else {
            await JavaApi.setStatus(problemId, status);
            showToast(`Marcado como “${STATUS_LABELS[status].replace(/^[★✓] /, '')}”.`, 'success');
        }
        await reloadProblem(problemId);
    } catch (err) {
        showToast(err.message, 'error');
        btn.disabled = false;
    }
}

async function handleDifficultyRate(problem, value) {
    try {
        if (problem.userDifficulty === null || problem.userDifficulty === undefined) {
            await JavaApi.rateDifficultyCreate(problem.id, value);
        } else {
            await JavaApi.rateDifficultyUpdate(problem.id, value);
        }
        showToast(`Avaliação salva: ${value} · ${difficultyLabel(value)}.`, 'success');
        await reloadProblem(problem.id);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function init() {
    const id = getProblemId();
    const content = document.getElementById('page-content');
    if (!id) {
        renderEmpty(content, 'Nenhum problema informado', 'Escolha um problema no catálogo.',
            '<a class="btn btn-secondary btn-sm mt-3" href="/pages/catalog.html">Ir para o catálogo</a>');
        return;
    }
    try {
        const problem = await JavaApi.getProblem(id);
        renderProblem(problem);
    } catch (err) {
        if (err.status === 404 || err.status === 400) {
            renderEmpty(content, 'Problema não encontrado', err.message,
                '<a class="btn btn-secondary btn-sm mt-3" href="/pages/catalog.html">Voltar ao catálogo</a>');
        } else {
            renderErrorAlert(content, err.message);
        }
    }
}

init();
