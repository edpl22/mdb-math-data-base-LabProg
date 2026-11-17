const examState = { area: '', minDifficulty: 1, maxDifficulty: 5, poolTotal: null };

async function loadAreaOptions() {
    try {
        const areas = await JavaApi.listAreas();
        const select = document.getElementById('area');
        areas.forEach((a) => {
            const opt = document.createElement('option');
            opt.value = a.name;
            opt.textContent = a.name;
            select.appendChild(opt);
        });
    } catch (err) {
        showToast('Não foi possível carregar as áreas: ' + err.message, 'error');
    }
}

async function refreshComposition() {
    const el = document.getElementById('composition-preview');
    renderLoading(el, 'Verificando problemas disponíveis…');

    const levels = [];
    for (let lvl = examState.minDifficulty; lvl <= examState.maxDifficulty; lvl++) levels.push(lvl);

    try {
        const counts = await Promise.all(levels.map((lvl) =>
            JavaApi.searchProblems({ area: examState.area || undefined, minDifficulty: lvl, maxDifficulty: lvl, size: 1 })
                .then((p) => p.totalElements)
        ));
        const total = counts.reduce((a, b) => a + b, 0);
        examState.poolTotal = total;

        el.innerHTML = levels.map((lvl, i) => `
            <div class="composition-row">
                <span class="level-tag">NÍVEL ${lvl} · ${difficultyLabel(lvl).toUpperCase()}</span>
                <span class="level-count ${counts[i] === 0 ? 'zero' : ''}">${counts[i]}</span>
            </div>`).join('');

        document.getElementById('pool-total-hint').textContent =
            `${plural(total, 'problema disponível', 'problemas disponíveis')} com os filtros atuais`;
        validateQuantity();
    } catch (err) {
        renderErrorAlert(el, err.message);
    }
}

function validateQuantity() {
    const input = document.getElementById('quantity');
    const qty = parseInt(input.value, 10);
    const hint = document.getElementById('pool-total-hint');
    const tooMany = examState.poolTotal !== null && qty > examState.poolTotal;
    hint.classList.toggle('text-danger', tooMany);
    if (tooMany) {
        hint.textContent = `Só há ${examState.poolTotal} disponíveis. Reduza a quantidade ou amplie os filtros.`;
    } else if (examState.poolTotal !== null) {
        hint.textContent = `${plural(examState.poolTotal, 'problema disponível', 'problemas disponíveis')} com os filtros atuais`;
    }
}

function onMaxSelect(v) {
    examState.maxDifficulty = v;
    if (examState.minDifficulty > v) examState.minDifficulty = v;
    renderSelectors();
    refreshComposition();
}

function onMinSelect(v) {
    examState.minDifficulty = v;
    if (examState.maxDifficulty < v) examState.maxDifficulty = v;
    renderSelectors();
    refreshComposition();
}

function renderSelectors() {
    renderDifficultySelector('min-difficulty-select', examState.minDifficulty, onMinSelect);
    renderDifficultySelector('max-difficulty-select', examState.maxDifficulty, onMaxSelect);
}

function renderExam(problems, title) {
    const resultsEl = document.getElementById('exam-results');
    const showAnswers = document.getElementById('toggle-answers').checked;
    const today = new Date().toLocaleDateString('pt-BR');
    const areaLabel = examState.area || 'Todas as áreas';

    resultsEl.innerHTML = `
        <article class="exam-sheet">
            <header class="exam-head">
                <div>
                    <div class="label-mono">MDB · Math Data Base</div>
                    <h2 class="exam-title">${escapeHtml(title)}</h2>
                    <div class="exam-meta">${escapeHtml(areaLabel)} · Dificuldade ${examState.minDifficulty}–${examState.maxDifficulty} · ${plural(problems.length, 'questão', 'questões')} · ${today}</div>
                </div>
                <div class="exam-name-line">Nome: ______________________________</div>
            </header>
            <ol class="exam-questions">
                ${problems.map((p) => `
                    <li class="exam-question">
                        <div class="exam-q-head">
                            <a class="exam-q-title" href="/pages/problem.html?id=${p.id}">${escapeHtml(p.title)}</a>
                            <span class="exam-q-meta">${escapeHtml(p.area)} · nível ${p.initialDifficulty}</span>
                        </div>
                        <div class="statement-block">${escapeHtml(p.statement || '')}</div>
                    </li>`).join('')}
            </ol>
            <section class="exam-answers ${showAnswers ? '' : 'hidden'}" id="exam-answers">
                <h3>Gabarito</h3>
                <ol>
                    ${problems.map((p) => `
                        <li>
                            <strong>${escapeHtml(p.title)}</strong>
                            <div class="answer-hint"><span class="label-mono">Dica</span> ${escapeHtml(p.hint || '—')}</div>
                            <div class="answer-solution"><span class="label-mono">Solução</span> ${escapeHtml(p.solution || '—')}</div>
                        </li>`).join('')}
                </ol>
            </section>
        </article>`;

    document.getElementById('exam-summary').textContent =
        `${plural(problems.length, 'questão sorteada', 'questões sorteadas')}, sem repetição.`;
    typesetMath(resultsEl);
}

async function generate() {
    const alertBox = document.getElementById('form-alert');
    const resultsSection = document.getElementById('exam-results-section');
    const resultsEl = document.getElementById('exam-results');
    alertBox.innerHTML = '';

    const quantity = parseInt(document.getElementById('quantity').value, 10);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 100) {
        renderErrorAlert(alertBox, 'Informe uma quantidade entre 1 e 100.');
        return;
    }

    const payload = {
        quantity,
        area: examState.area || null,
        minDifficulty: examState.minDifficulty,
        maxDifficulty: examState.maxDifficulty,
        includeStatements: true,
    };
    const title = document.getElementById('exam-title').value.trim() || 'Lista de exercícios';

    resultsSection.classList.remove('hidden');
    renderLoading(resultsEl, 'Gerando simulado…');

    const btn = document.getElementById('generate-btn');
    try {
        const response = await withButtonLoading(btn, 'Gerando…', () => PythonApi.mockExam(payload));
        if (response.problems.length === 0) {
            renderEmpty(resultsEl, 'Nenhum problema selecionado', 'Ajuste os filtros e tente novamente.');
            return;
        }

        renderExam(response.problems, title);
        resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (err) {
        renderErrorAlert(alertBox, err.message);
        resultsSection.classList.add('hidden');
    }
}

document.getElementById('area').addEventListener('change', (evt) => {
    examState.area = evt.target.value;
    refreshComposition();
});
document.getElementById('quantity').addEventListener('input', validateQuantity);

document.getElementById('mock-exam-form').addEventListener('submit', (evt) => {
    evt.preventDefault();
    generate();
});
document.getElementById('regenerate-btn').addEventListener('click', generate);
document.getElementById('print-btn').addEventListener('click', () => window.print());
document.getElementById('toggle-answers').addEventListener('change', (evt) => {
    const answers = document.getElementById('exam-answers');
    if (answers) {
        answers.classList.toggle('hidden', !evt.target.checked);
        if (evt.target.checked) typesetMath(answers);
    }
});
loadAreaOptions();
renderSelectors();
refreshComposition();
