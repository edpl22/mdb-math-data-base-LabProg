if (!Auth.isAdmin()) {
    window.location.replace(Auth.isLoggedIn() ? '/index.html' : '/pages/login.html?next=/pages/admin.html');
    throw new Error('Acesso restrito a administradores.');
}

// navegação lateral
document.querySelectorAll('#admin-nav button').forEach((btn) => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('#admin-nav button').forEach((b) => b.classList.remove('active'));
        document.querySelectorAll('.admin-panel').forEach((p) => p.classList.remove('active'));
        btn.classList.add('active');
        document.getElementById(`panel-${btn.dataset.panel}`).classList.add('active');
    });
});

// visão geral
async function loadOverview() {
    const el = document.getElementById('overview-stats');
    try {
        const [problems, areas, tags] = await Promise.all([
            JavaApi.searchProblems({ size: 1 }),
            JavaApi.listAreas(),
            JavaApi.listTags(),
        ]);
        el.innerHTML = `
            <div class="metadata-item"><span class="m-label">Problemas</span><span class="m-value">${problems.totalElements}</span></div>
            <div class="metadata-item"><span class="m-label">Áreas</span><span class="m-value">${areas.length}</span></div>
            <div class="metadata-item"><span class="m-label">Tópicos</span><span class="m-value">${tags.length}</span></div>
        `;
    } catch (err) {
        renderErrorAlert(el, err.message);
    }
}

// problemas
let allAreas = [];

async function loadAreasIntoSelects() {
    allAreas = await JavaApi.listAreas();
    const selects = [document.getElementById('p-area'), document.getElementById('export-area')];
    selects.forEach((select) => {
        const keepFirst = select.id === 'export-area';
        select.innerHTML = keepFirst ? '<option value="">Todas as áreas</option>' : '';
        allAreas.forEach((a) => {
            const opt = document.createElement('option');
            opt.value = a.name;
            opt.textContent = a.name;
            select.appendChild(opt);
        });
    });
}

function relativeDate(isoString) {
    if (!isoString) return '—';
    const diffMs = Date.now() - new Date(isoString).getTime();
    const days = Math.floor(diffMs / 86400000);
    if (days <= 0) return 'Hoje';
    if (days === 1) return 'Ontem';
    if (days < 30) return `há ${days} dias`;
    return new Date(isoString).toLocaleDateString('pt-BR');
}

async function loadProblemsTable() {
    const tbody = document.getElementById('problems-table-body');
    tbody.innerHTML = '<tr><td colspan="6">Carregando…</td></tr>';
    try {
        const page = await JavaApi.searchProblems({ size: 100 });
        document.getElementById('problems-count').textContent = `${page.totalElements} problema(s) cadastrado(s)`;
        if (page.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6">Nenhum problema cadastrado ainda.</td></tr>';
            return;
        }
        tbody.innerHTML = page.content.map((p) => `
            <tr>
                <td class="mono">#${p.id}</td>
                <td><a href="/pages/problem.html?id=${p.id}" target="_blank">${escapeHtml(p.title)}</a></td>
                <td>${escapeHtml(p.area)}</td>
                <td class="mono">${p.initialDifficulty} / 5</td>
                <td class="mono">${relativeDate(p.updatedAt)}</td>
                <td class="row-actions">
                    <button class="btn btn-secondary btn-sm" data-edit="${p.id}">Editar</button>
                    <button class="btn btn-danger btn-sm" data-delete="${p.id}">Excluir</button>
                </td>
            </tr>`).join('');

        tbody.querySelectorAll('[data-edit]').forEach((btn) =>
            btn.addEventListener('click', () => openEditForm(btn.dataset.edit)));
        tbody.querySelectorAll('[data-delete]').forEach((btn) =>
            btn.addEventListener('click', () => deleteProblem(btn.dataset.delete)));
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6">Erro: ${escapeHtml(err.message)}</td></tr>`;
    }
}

function showProblemForm() {
    document.getElementById('problem-form-card').classList.remove('hidden');
    document.getElementById('problem-form-card').scrollIntoView({ behavior: 'smooth' });
}

let currentFormDifficulty = 3;

function resetProblemForm() {
    document.getElementById('problem-form').reset();
    document.getElementById('p-id').value = '';
    document.getElementById('problem-form-title').textContent = 'Novo problema';
    document.getElementById('problem-form-alert').innerHTML = '';
    setDifficultySelector(3);
}

document.getElementById('new-problem-btn').addEventListener('click', () => {
    resetProblemForm();
    showProblemForm();
});
document.getElementById('cancel-problem-form').addEventListener('click', () => {
    document.getElementById('problem-form-card').classList.add('hidden');
    resetProblemForm();
});

async function openEditForm(id) {
    try {
        const problem = await JavaApi.getProblem(id);
        document.getElementById('problem-form-title').textContent = `Editar: ${problem.title}`;
        document.getElementById('p-id').value = problem.id;
        document.getElementById('p-title').value = problem.title;
        document.getElementById('p-statement').value = problem.statement;
        document.getElementById('p-area').value = problem.area;
        document.getElementById('p-tags').value = (problem.tags || []).join(', ');
        document.getElementById('p-hint').value = problem.hint;
        document.getElementById('p-solution').value = problem.solution;
        document.getElementById('p-source').value = problem.source || '';
        currentFormDifficulty = problem.initialDifficulty;
        setDifficultySelector(currentFormDifficulty);
        showProblemForm();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function setDifficultySelector(value) {
    currentFormDifficulty = value;
    renderDifficultySelector('p-difficulty-select', currentFormDifficulty, setDifficultySelector);
}

async function deleteProblem(id) {
    if (!confirm('Excluir este problema? Esta ação não pode ser desfeita.')) return;
    try {
        await JavaApi.deleteProblem(id);
        showToast('Problema excluído.', 'success');
        loadProblemsTable();
        loadOverview();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

document.getElementById('problem-form').addEventListener('submit', async (evt) => {
    evt.preventDefault();
    const alertBox = document.getElementById('problem-form-alert');
    alertBox.innerHTML = '';

    const id = document.getElementById('p-id').value;
    const payload = {
        title: document.getElementById('p-title').value.trim(),
        statement: document.getElementById('p-statement').value.trim(),
        area: document.getElementById('p-area').value,
        difficulty: currentFormDifficulty,
        tags: document.getElementById('p-tags').value.split(',').map((t) => t.trim()).filter(Boolean),
        hint: document.getElementById('p-hint').value.trim(),
        solution: document.getElementById('p-solution').value.trim(),
        source: document.getElementById('p-source').value.trim() || null,
    };

    try {
        if (id) {
            await JavaApi.updateProblem(id, payload);
            showToast('Problema atualizado.', 'success');
        } else {
            await JavaApi.createProblem(payload);
            showToast('Problema criado.', 'success');
        }
        document.getElementById('problem-form-card').classList.add('hidden');
        resetProblemForm();
        loadProblemsTable();
        loadOverview();
    } catch (err) {
        renderErrorAlert(alertBox, err.message);
    }
});

// áreas
async function loadAreasList() {
    const el = document.getElementById('areas-list');
    try {
        const areas = await JavaApi.listAreas();
        allAreas = areas;
        el.innerHTML = areas.map((a) => `<div class="index-row"><span class="index-name">${escapeHtml(a.name)}</span></div>`).join('');
    } catch (err) {
        renderErrorAlert(el, err.message);
    }
}

document.getElementById('area-form').addEventListener('submit', async (evt) => {
    evt.preventDefault();
    const alertBox = document.getElementById('area-form-alert');
    alertBox.innerHTML = '';
    const name = document.getElementById('area-name').value.trim();
    try {
        await JavaApi.createArea(name);
        showToast('Área criada.', 'success');
        document.getElementById('area-form').reset();
        await Promise.all([loadAreasList(), loadAreasIntoSelects()]);
        loadOverview();
    } catch (err) {
        renderErrorAlert(alertBox, err.message);
    }
});

// tópicos
async function loadTagsList() {
    const el = document.getElementById('tags-list');
    try {
        const tags = await JavaApi.listTags();
        if (tags.length === 0) {
            renderEmpty(el, 'Nenhum tópico ainda', 'Os tópicos são criados automaticamente ao serem usados em um problema.');
            return;
        }
        el.innerHTML = tags.map((t) => `<span class="tag">${escapeHtml(t.name)}</span>`).join('');
    } catch (err) {
        renderErrorAlert(el, err.message);
    }
}

// CSV (API Python)
document.getElementById('import-form').addEventListener('submit', async (evt) => {
    evt.preventDefault();
    const alertBox = document.getElementById('import-alert');
    const resultsBox = document.getElementById('import-results');
    alertBox.innerHTML = '';
    resultsBox.innerHTML = '';
    const fileInput = document.getElementById('csv-file');
    const file = fileInput.files[0];
    if (!file) return;

    try {
        const result = await PythonApi.importCsv(file);
        showToast(`Importação concluída: ${result.imported} inserido(s), ${result.failed} com erro.`, result.failed ? 'info' : 'success');
        let html = `<div class="alert alert-success"><strong>${result.imported}</strong> de <strong>${result.totalRows}</strong> linha(s) importada(s) com sucesso.</div>`;
        if (result.errors.length > 0) {
            html += '<div class="alert alert-error"><strong>Linhas com erro:</strong><ul>';
            html += result.errors.map((e) => `<li>Linha ${e.line}: ${escapeHtml(e.reason)}</li>`).join('');
            html += '</ul></div>';
        }
        resultsBox.innerHTML = html;
        loadProblemsTable();
        loadOverview();
    } catch (err) {
        renderErrorAlert(alertBox, err.message);
    }
});

document.getElementById('export-btn').addEventListener('click', async () => {
    const area = document.getElementById('export-area').value;
    try {
        const blob = await PythonApi.exportCsv({ area: area || undefined });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'mdb_problemas_export.csv';
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        showToast('Exportação concluída.', 'success');
    } catch (err) {
        showToast(err.message, 'error');
    }
});

loadOverview();
loadProblemsTable();
loadAreasList();
loadAreasIntoSelects();
loadTagsList();
resetProblemForm();
