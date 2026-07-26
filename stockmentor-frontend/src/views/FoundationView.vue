<template>
  <main class="foundation-page">
    <section aria-labelledby="foundation-title">
      <header>
        <h1 id="foundation-title">StockMentor</h1>
        <p class="description">
          An investment education application for learning foundational concepts and analysis
          frameworks. It does not provide investment advice, short-term price predictions, or
          return claims.
        </p>
      </header>

      <dl>
        <div>
          <dt>Current version</dt>
          <dd>V0.1</dd>
        </div>
        <div>
          <dt>Backend health</dt>
          <dd><code>/api/v1/system/health</code></dd>
        </div>
      </dl>

      <p class="status">Business modules are not yet implemented.</p>
    </section>
  </main>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  min-width: 320px;
  margin: 0;
  color: #1f2937;
  background: #ffffff;
  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI",
    sans-serif;
}

.foundation-page {
  display: grid;
  min-height: 100svh;
  padding: 2rem;
  place-items: center;
}

section {
  width: min(100%, 42rem);
}

h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(2rem, 7vw, 3rem);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.1;
}

.description {
  max-width: 38rem;
  margin: 1rem 0 0;
  font-size: 1rem;
  line-height: 1.65;
}

dl {
  display: grid;
  gap: 1rem;
  margin: 2rem 0;
}

dt {
  color: #4b5563;
  font-size: 0.875rem;
  font-weight: 600;
}

dd {
  margin: 0.25rem 0 0;
  color: #111827;
  font-size: 1rem;
}

code {
  overflow-wrap: anywhere;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
}

.status {
  margin: 0;
  color: #4b5563;
  font-size: 0.9375rem;
}

@media (max-width: 480px) {
  .foundation-page {
    padding: 1.5rem;
    place-items: start center;
  }
}
</style>
