import { StatusBadge } from './StatusBadge';

export function SummaryPanel({ file, summaryState }) {
  if (!file) {
    return (
      <section className="glass-panel summary-panel">
        <p className="section-label">Summary</p>
        <h2>No file selected</h2>
        <p className="muted-copy">
          Choose a file from the library to inspect its generated summary and
          processing details.
        </p>
      </section>
    );
  }

  const { isLoading, error, data } = summaryState;

  return (
    <section className="glass-panel summary-panel">
      <div className="panel-header">
        <div>
          <p className="section-label">Summary</p>
          <h2>{file.originalName}</h2>
        </div>
        <StatusBadge status={file.processingStatus} />
      </div>

      {isLoading ? <p className="muted-copy">Refreshing generated summary...</p> : null}
      {error ? <p className="inline-error">{error}</p> : null}

      <div className="summary-body">
        {data ? (
          <p>{data}</p>
        ) : (
          <p className="muted-copy">
            {file.processingStatus === 'READY'
              ? 'This file is ready, but no summary text is stored yet.'
              : 'Process this file to generate a backend summary.'}
          </p>
        )}
      </div>

      <div className="summary-facts">
        <div>
          <span className="fact-label">Category</span>
          <strong>{file.fileCategory}</strong>
        </div>
        <div>
          <span className="fact-label">Content type</span>
          <strong>{file.contentType}</strong>
        </div>
      </div>
    </section>
  );
}
