import { useRef, useState } from 'react';

export function UploadPanel({ isUploading, uploadError, onUpload }) {
  const inputRef = useRef(null);
  const [dragActive, setDragActive] = useState(false);

  function handleFileSelection(file) {
    if (!file || isUploading) {
      return;
    }

    void onUpload(file);
  }

  return (
    <section className={`glass-panel upload-panel ${dragActive ? 'drag-active' : ''}`}>
      <div>
        <p className="section-label">Phase 6</p>
        <h2>Upload another file</h2>
        <p className="muted-copy">
          Drop a PDF, MP3, MP4, WAV, M4A, MPEG, MPGA, or WebM file to start the
          ingestion flow.
        </p>
      </div>

      <div
        className="upload-dropzone"
        onDragEnter={(event) => {
          event.preventDefault();
          setDragActive(true);
        }}
        onDragOver={(event) => {
          event.preventDefault();
          setDragActive(true);
        }}
        onDragLeave={(event) => {
          event.preventDefault();
          setDragActive(false);
        }}
        onDrop={(event) => {
          event.preventDefault();
          setDragActive(false);
          handleFileSelection(event.dataTransfer.files?.[0]);
        }}
      >
        <input
          ref={inputRef}
          className="sr-only"
          type="file"
          accept=".pdf,.mp3,.mp4,.wav,.m4a,.mpeg,.mpga,.webm"
          onChange={(event) => {
            handleFileSelection(event.target.files?.[0]);
            event.target.value = '';
          }}
        />

        <p className="upload-title">
          {isUploading ? 'Uploading file...' : 'Drag and drop your file here'}
        </p>
        <p className="upload-subtitle">
          Or pick a file manually and let the backend store metadata, content,
          transcript segments, and summaries.
        </p>

        <button
          type="button"
          className="action-button accent-button"
          disabled={isUploading}
          onClick={() => inputRef.current?.click()}
        >
          {isUploading ? 'Uploading...' : 'Choose file'}
        </button>
      </div>

      {uploadError ? <p className="inline-error">{uploadError}</p> : null}
    </section>
  );
}
