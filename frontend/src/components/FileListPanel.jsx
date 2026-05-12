import { StatusBadge } from './StatusBadge';

function formatDate(value) {
  if (!value) {
    return 'Unknown time';
  }

  return new Intl.DateTimeFormat('en-IN', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function actionLabel(status, isProcessing) {
  if (isProcessing) {
    return 'Processing...';
  }

  if (status === 'READY') {
    return 'Reprocess';
  }

  if (status === 'PROCESSING') {
    return 'Processing...';
  }

  return 'Process';
}

export function FileListPanel({
  files,
  selectedFileId,
  processingFileId,
  onSelect,
  onProcess
}) {
  return (
    <section className="glass-panel file-list-panel">
      <div className="panel-header">
        <div>
          <p className="section-label">Library</p>
          <h2>Uploaded files</h2>
        </div>
        <span className="count-chip">{files.length} items</span>
      </div>

      {files.length === 0 ? (
        <div className="empty-card">
          <p className="empty-title">No files uploaded yet</p>
          <p className="muted-copy">
            Your uploaded PDFs and media files will appear here with processing
            state, summary status, and Q&A readiness.
          </p>
        </div>
      ) : (
        <div className="file-list">
          {files.map((file) => {
            const isSelected = file.id === selectedFileId;
            const isProcessing = processingFileId === file.id;

            return (
              <article
                key={file.id}
                className={`file-card ${isSelected ? 'selected' : ''}`}
                onClick={() => onSelect(file.id)}
              >
                <div className="file-card-top">
                  <div>
                    <p className="file-name">{file.originalName}</p>
                    <p className="file-meta">
                      {file.fileCategory} • {formatDate(file.createdAt)}
                    </p>
                  </div>
                  <StatusBadge status={file.processingStatus} />
                </div>

                <p className="file-summary-snippet">
                  {file.summary
                    ? file.summary
                    : 'No summary yet. Process this file to extract content and generate insights.'}
                </p>

                {file.processingError ? (
                  <p className="inline-error">{file.processingError}</p>
                ) : null}

                <div className="file-card-actions">
                  <button
                    type="button"
                    className="action-button ghost-button"
                    disabled={file.processingStatus === 'PROCESSING' || isProcessing}
                    onClick={(event) => {
                      event.stopPropagation();
                      void onProcess(file.id);
                    }}
                  >
                    {actionLabel(file.processingStatus, isProcessing)}
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
