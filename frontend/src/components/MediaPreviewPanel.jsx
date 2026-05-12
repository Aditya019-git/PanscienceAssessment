function formatClock(seconds) {
  if (seconds == null || Number.isNaN(seconds)) {
    return '--:--';
  }

  const rounded = Math.max(0, Math.floor(seconds));
  const minutes = Math.floor(rounded / 60);
  const remainingSeconds = rounded % 60;

  return `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`;
}

export function MediaPreviewPanel({
  file,
  mediaRef,
  latestPlaybackTime,
  onJumpToTimestamp
}) {
  if (!file) {
    return (
      <section className="glass-panel preview-panel">
        <p className="section-label">Preview</p>
        <h2>Workspace ready</h2>
        <p className="muted-copy">
          Select a file to preview it, inspect its metadata, and ask grounded
          questions from the chat workspace.
        </p>
      </section>
    );
  }

  const contentUrl = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'}/files/${file.id}/content`;
  const isMedia = file.fileCategory === 'AUDIO' || file.fileCategory === 'VIDEO';

  return (
    <section className="glass-panel preview-panel">
      <div className="panel-header">
        <div>
          <p className="section-label">Preview</p>
          <h2>{isMedia ? 'Media player' : 'File reference'}</h2>
        </div>
        <a className="text-link" href={contentUrl} target="_blank" rel="noreferrer">
          Open file
        </a>
      </div>

      {isMedia ? (
        <>
          {file.fileCategory === 'VIDEO' ? (
            <video ref={mediaRef} className="media-frame" controls src={contentUrl} />
          ) : (
            <audio ref={mediaRef} className="audio-frame" controls src={contentUrl} />
          )}

          <div className="timestamp-card">
            <div>
              <span className="fact-label">Latest answer cue</span>
              <strong>{formatClock(latestPlaybackTime)}</strong>
            </div>
            <button
              type="button"
              className="action-button ghost-button"
              disabled={latestPlaybackTime == null}
              onClick={() => onJumpToTimestamp(latestPlaybackTime)}
            >
              Play from cue
            </button>
          </div>
        </>
      ) : (
        <div className="pdf-card">
          <p>
            This PDF can be opened in a new tab, while the generated summary and
            question answering below stay grounded in the extracted chunks.
          </p>
        </div>
      )}
    </section>
  );
}
