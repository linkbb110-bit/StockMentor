<template>
  <div class="app-shell">
    <RouterView />
  </div>
</template>

<style>
:root {
  color: #1f2937;
  background: #f8fafc;
  font-family:
    Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  font-synthesis: none;
  text-rendering: optimizeLegibility;
}

* {
  box-sizing: border-box;
}

body {
  min-width: 320px;
  min-height: 100vh;
  margin: 0;
}

button,
input {
  font: inherit;
}

a {
  color: #1d4ed8;
}

.app-shell,
#app {
  min-height: 100vh;
}

.page {
  display: grid;
  min-height: 100vh;
  padding: 2rem 1rem;
  place-items: center;
}

.panel {
  width: min(100%, 32rem);
  padding: clamp(1.5rem, 5vw, 2.5rem);
  border: 1px solid #e2e8f0;
  border-radius: 1rem;
  background: #ffffff;
  box-shadow: 0 1rem 2.5rem rgb(15 23 42 / 8%);
}

.panel-header {
  margin-bottom: 1.5rem;
}

.panel-header h1,
.panel-header p {
  margin: 0;
}

.panel-header h1 {
  margin-top: 0.35rem;
  color: #0f172a;
  font-size: clamp(1.75rem, 6vw, 2.25rem);
  line-height: 1.2;
}

.panel-header h1 + p {
  margin-top: 0.65rem;
  color: #64748b;
  line-height: 1.6;
}

.eyebrow {
  color: #1d4ed8;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.form-grid,
.field {
  display: grid;
}

.form-grid {
  gap: 1rem;
}

.field {
  gap: 0.4rem;
}

.field label {
  color: #334155;
  font-size: 0.9rem;
  font-weight: 650;
}

.field input {
  width: 100%;
  min-height: 2.75rem;
  padding: 0.65rem 0.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.55rem;
  color: #0f172a;
  background: #ffffff;
}

.field input:focus-visible,
button:focus-visible,
a:focus-visible {
  outline: 3px solid rgb(59 130 246 / 30%);
  outline-offset: 2px;
}

.field input[readonly] {
  color: #475569;
  background: #f1f5f9;
}

.hint,
.feedback,
.secondary-action,
.notice {
  margin: 0;
  line-height: 1.5;
}

.hint {
  color: #64748b;
  font-size: 0.8rem;
}

.feedback {
  padding: 0.7rem 0.8rem;
  border-radius: 0.5rem;
  font-size: 0.9rem;
}

.feedback-error {
  color: #991b1b;
  background: #fef2f2;
}

.feedback-success {
  color: #166534;
  background: #f0fdf4;
}

button,
.button-link {
  display: inline-flex;
  min-height: 2.75rem;
  align-items: center;
  justify-content: center;
  padding: 0.65rem 1rem;
  border: 1px solid #1d4ed8;
  border-radius: 0.55rem;
  color: #ffffff;
  background: #1d4ed8;
  cursor: pointer;
  font-weight: 650;
  text-decoration: none;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.button-secondary {
  color: #1d4ed8;
  background: #ffffff;
}

.secondary-action,
.notice {
  margin-top: 1.25rem;
  color: #475569;
  text-align: center;
}

.notice {
  padding-top: 1rem;
  border-top: 1px solid #e2e8f0;
  font-size: 0.8rem;
}

.identity-list {
  display: grid;
  gap: 1rem;
  margin: 0 0 1.5rem;
}

.identity-list dt {
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 650;
}

.identity-list dd {
  margin: 0.25rem 0 0;
  overflow-wrap: anywhere;
  color: #0f172a;
}

.placeholder {
  padding: 1rem;
  border-radius: 0.6rem;
  color: #334155;
  background: #f1f5f9;
}

.placeholder p {
  margin: 0;
}

.placeholder p + p {
  margin-top: 0.4rem;
}

.actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.full-width {
  width: 100%;
  margin-top: 0.75rem;
}

@media (max-width: 420px) {
  .page {
    padding: 1rem;
    place-items: start center;
  }

  .actions {
    grid-template-columns: 1fr;
  }
}
</style>
