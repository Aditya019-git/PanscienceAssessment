import { useState } from 'react';

function formatTimestamp(seconds) {
  if (seconds == null || Number.isNaN(seconds)) {
    return null;
  }

  const rounded = Math.max(0, Math.floor(seconds));
  const minutes = Math.floor(rounded / 60);
  const remainingSeconds = rounded % 60;
  return `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`;
}

function sourceMeta(source) {
  if (source.pageNumber != null) {
    return `Page ${source.pageNumber}`;
  }

  if (source.startTime != null) {
    const start = formatTimestamp(source.startTime);
    const end = formatTimestamp(source.endTime ?? source.startTime);
    return `${start} - ${end}`;
  }

  return 'Retrieved excerpt';
}

export function QuestionPanel({
  file,
  isAsking,
  history,
  askError,
  onAsk,
  onJumpToTimestamp
}) {
  const [question, setQuestion] = useState('');

  if (!file) {
    return (
      <section className="glass-panel qa-panel">
        <p className="section-label">Grounded Q&A</p>
        <h2>Choose a file to start chatting</h2>
        <p className="muted-copy">
          The chat workspace will show answers, source citations, and media cue
          buttons here.
        </p>
      </section>
    );
  }

  const disabled = file.processingStatus !== 'READY' || isAsking;

  return (
    <section className="glass-panel qa-panel">
      <div className="panel-header">
        <div>
          <p className="section-label">Grounded Q&A</p>
          <h2>Ask about {file.originalName}</h2>
        </div>
      </div>

      <form
        className="question-form"
        onSubmit={(event) => {
          event.preventDefault();
          const trimmedQuestion = question.trim();
          if (!trimmedQuestion || disabled) {
            return;
          }

          void onAsk(trimmedQuestion);
          setQuestion('');
        }}
      >
        <textarea
          value={question}
          className="question-input"
          placeholder={
            file.processingStatus === 'READY'
              ? 'Ask a grounded question about this file...'
              : 'Process the file first to enable grounded Q&A.'
          }
          disabled={disabled}
          onChange={(event) => setQuestion(event.target.value)}
        />
        <div className="question-form-footer">
          <p className="muted-copy">
            Answers are grounded in extracted chunks and transcript segments.
          </p>
          <button
            type="submit"
            className="action-button accent-button"
            disabled={disabled || !question.trim()}
          >
            {isAsking ? 'Thinking...' : 'Ask question'}
          </button>
        </div>
      </form>

      {askError ? <p className="inline-error">{askError}</p> : null}

      <div className="chat-history">
        {history.length === 0 ? (
          <div className="empty-card">
            <p className="empty-title">No questions yet</p>
            <p className="muted-copy">
              Ask for key takeaways, page references, or media timestamps once
              the file reaches the ready state.
            </p>
          </div>
        ) : (
          history.map((entry) => (
            <article key={entry.id} className="qa-card">
              <p className="qa-question">{entry.question}</p>
              <p className="qa-answer">{entry.answer}</p>

              {entry.suggestedPlaybackStartTime != null ? (
                <button
                  type="button"
                  className="action-button ghost-button"
                  onClick={() => onJumpToTimestamp(entry.suggestedPlaybackStartTime)}
                >
                  Play from {formatTimestamp(entry.suggestedPlaybackStartTime)}
                </button>
              ) : null}

              {entry.sources?.length ? (
                <div className="source-list">
                  {entry.sources.map((source) => (
                    <div key={`${entry.id}-${source.sourceNumber}`} className="source-card">
                      <div className="source-card-top">
                        <strong>[{source.sourceNumber}]</strong>
                        <span>{sourceMeta(source)}</span>
                      </div>
                      <p>{source.excerpt}</p>
                      {source.startTime != null ? (
                        <button
                          type="button"
                          className="text-link button-link"
                          onClick={() => onJumpToTimestamp(source.startTime)}
                        >
                          Jump to {formatTimestamp(source.startTime)}
                        </button>
                      ) : null}
                    </div>
                  ))}
                </div>
              ) : null}
            </article>
          ))
        )}
      </div>
    </section>
  );
}
