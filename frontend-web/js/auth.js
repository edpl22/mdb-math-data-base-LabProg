function safeNextUrl() {
    // só aceita caminhos internos
    const next = new URLSearchParams(window.location.search).get('next');
    return next && next.startsWith('/') && !next.startsWith('//') ? next : '/index.html';
}

function afterAuthSuccess(response) {
    Auth.setSession(response.token, response.user);
    showToast(`Bem-vindo(a), ${response.user.name.split(' ')[0]}!`, 'success');
    setTimeout(() => { window.location.href = safeNextUrl(); }, 400);
}

function formErrorMessage(err) {
    if (err.details && err.details.length > 1) {
        return err.details.map((d) => d.replace(/^[^:]+: /, '')).join(' · ');
    }
    return err.message;
}

if (Auth.isLoggedIn()) {
    window.location.replace(safeNextUrl());
}

const loginForm = document.getElementById('login-form');
if (loginForm) {
    loginForm.addEventListener('submit', async (evt) => {
        evt.preventDefault();
        const alertBox = document.getElementById('form-alert');
        alertBox.innerHTML = '';
        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const btn = loginForm.querySelector('button[type="submit"]');
        try {
            const response = await withButtonLoading(btn, 'Entrando…', () => JavaApi.login(email, password));
            afterAuthSuccess(response);
        } catch (err) {
            renderErrorAlert(alertBox, formErrorMessage(err));
        }
    });

    document.querySelectorAll('[data-demo]').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.getElementById('email').value = btn.dataset.demo;
            document.getElementById('password').value = btn.dataset.pass;
            loginForm.requestSubmit();
        });
    });
}

const registerForm = document.getElementById('register-form');
if (registerForm) {
    registerForm.addEventListener('submit', async (evt) => {
        evt.preventDefault();
        const alertBox = document.getElementById('form-alert');
        alertBox.innerHTML = '';
        const name = document.getElementById('name').value.trim();
        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const btn = registerForm.querySelector('button[type="submit"]');
        try {
            const response = await withButtonLoading(btn, 'Criando conta…', () => JavaApi.register(name, email, password));
            afterAuthSuccess(response);
        } catch (err) {
            renderErrorAlert(alertBox, formErrorMessage(err));
        }
    });
}
