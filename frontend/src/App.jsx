const milestones = [
  'Upload PDFs, audio, and video files',
  'Extract PDF text and timestamped transcripts',
  'Answer questions with grounded context',
  'Generate summaries and media timestamps',
  'Add streaming, JWT auth, and Redis'
];

function App() {
  return (
    <main className="app-shell">
      <section className="hero-card">
        <p className="eyebrow">Panscience SDE-1 Assessment</p>
        <h1>AI-powered document and multimedia Q&amp;A platform</h1>
        <p className="hero-copy">
          The project scaffold is ready. Next we will wire uploads, processing,
          retrieval, streaming responses, authentication, and Redis-backed
          controls.
        </p>
      </section>

      <section className="grid">
        <article className="panel">
          <h2>Stack</h2>
          <ul>
            <li>Spring Boot backend</li>
            <li>React + Vite frontend</li>
            <li>PostgreSQL metadata store</li>
            <li>OpenAI + Whisper integration</li>
            <li>Docker + GitHub Actions</li>
          </ul>
        </article>

        <article className="panel">
          <h2>Roadmap</h2>
          <ol>
            {milestones.map((milestone) => (
              <li key={milestone}>{milestone}</li>
            ))}
          </ol>
        </article>
      </section>
    </main>
  );
}

export default App;
