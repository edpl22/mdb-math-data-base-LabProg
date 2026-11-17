// Inspetor de requisições, disponível só para administradores (Ctrl+I).

(function () {
    if (!Auth.isAdmin()) return;

    const PREF_KEY = 'mdb_inspector_open';
    let filter = 'all';

    function buildPanel() {
        const panel = document.createElement('aside');
        panel.id = 'inspector';
        panel.className = 'inspector';
        panel.setAttribute('aria-label', 'Inspetor de requisições');
        panel.hidden = true;
        panel.innerHTML = `
            <div class="inspector-header">
                <div>
                    <div class="inspector-title">Inspetor de requisições</div>
                    <div class="inspector-sub">Ctrl+I abre e fecha</div>
                </div>
                <button type="button" class="btn btn-secondary btn-sm" id="inspector-close" aria-label="Fechar">✕</button>
            </div>
            <div class="inspector-summary" id="inspector-summary"></div>
            <div class="inspector-filters">
                <button type="button" data-filter="all" class="active">Todas</button>
                <button type="button" data-filter="java">Java</button>
                <button type="button" data-filter="python">Python</button>
                <button type="button" class="inspector-clear" id="inspector-clear">Limpar</button>
            </div>
            <ol class="inspector-list" id="inspector-list"></ol>`;
        document.body.appendChild(panel);
    }

    function render() {
        const list = document.getElementById('inspector-list');
        if (!list || document.getElementById('inspector').hidden) return;
        const entries = ApiLog.read();
        const javaCount = entries.filter((e) => e.api === 'java').length;
        document.getElementById('inspector-summary').innerHTML = `
            <div class="summary-cell summary-java"><span class="n">${javaCount}</span><span>API Java</span></div>
            <div class="summary-cell summary-python"><span class="n">${entries.length - javaCount}</span><span>API Python</span></div>`;

        const shown = entries.filter((e) => filter === 'all' || e.api === filter).reverse();
        if (shown.length === 0) {
            list.innerHTML = '<li class="inspector-empty">Nenhuma requisição registrada ainda.</li>';
            return;
        }
        list.innerHTML = shown.map((e) => `
            <li class="inspector-item">
                <span class="api-badge api-badge-${e.api}">${e.api === 'java' ? 'JAVA' : 'PYTHON'}</span>
                <span class="inspector-method m-${e.method.toLowerCase()}">${e.method}</span>
                <span class="inspector-path" title="${escapeHtml(e.path)}">${escapeHtml(e.path)}</span>
                <span class="inspector-status ${e.ok ? 'ok' : 'err'}">${e.status || 'falha'}</span>
                <span class="inspector-meta">${new Date(e.at).toLocaleTimeString('pt-BR')} · ${e.ms} ms · ${escapeHtml(e.page)}</span>
            </li>`).join('');
    }

    function setOpen(open) {
        document.getElementById('inspector').hidden = !open;
        document.body.classList.toggle('inspector-open', open);
        try { sessionStorage.setItem(PREF_KEY, open ? '1' : '0'); } catch { /* sem storage */ }
        render();
    }

    document.addEventListener('DOMContentLoaded', () => {
        buildPanel();

        document.getElementById('inspector-close').addEventListener('click', () => setOpen(false));
        document.getElementById('inspector-clear').addEventListener('click', () => ApiLog.clear());
        document.querySelectorAll('.inspector-filters [data-filter]').forEach((btn) => {
            btn.addEventListener('click', () => {
                filter = btn.dataset.filter;
                document.querySelectorAll('.inspector-filters [data-filter]')
                    .forEach((b) => b.classList.toggle('active', b === btn));
                render();
            });
        });

        document.addEventListener('keydown', (evt) => {
            const panel = document.getElementById('inspector');
            if ((evt.ctrlKey || evt.metaKey) && evt.key.toLowerCase() === 'i' && !evt.shiftKey) {
                evt.preventDefault();
                setOpen(panel.hidden);
            } else if (evt.key === 'Escape' && !panel.hidden) {
                setOpen(false);
            }
        });

        window.addEventListener('mdb:api-call', render);

        let wasOpen = false;
        try { wasOpen = sessionStorage.getItem(PREF_KEY) === '1'; } catch { /* sem storage */ }
        if (wasOpen) setOpen(true);
    });
})();
