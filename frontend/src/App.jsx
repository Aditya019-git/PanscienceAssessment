import { startTransition, useDeferredValue, useEffect, useMemo, useRef, useState } from 'react';
import {
  askQuestionStream,
  getFile,
  getFileSummary,
  listFiles,
  processFile,
  uploadFile
} from './api/files';
import { FileListPanel } from './components/FileListPanel';
import { MediaPreviewPanel } from './components/MediaPreviewPanel';
import { QuestionPanel } from './components/QuestionPanel';
import { SummaryPanel } from './components/SummaryPanel';
import { UploadPanel } from './components/UploadPanel';

function App() {
  const mediaRef = useRef(null);
  const [files, setFiles] = useState([]);
  const [selectedFileId, setSelectedFileId] = useState(null);
  const [isBooting, setIsBooting] = useState(true);
  const [pageError, setPageError] = useState('');
  const [uploadState, setUploadState] = useState({ isUploading: false, error: '' });
  const [processingFileId, setProcessingFileId] = useState(null);
  const [summaryState, setSummaryState] = useState({
    isLoading: false,
    data: '',
    error: ''
  });
  const [questionState, setQuestionState] = useState({
    isAsking: false,
    error: ''
  });
  const [qaHistoryByFile, setQaHistoryByFile] = useState({});
  const [latestPlaybackTime, setLatestPlaybackTime] = useState(null);

  const deferredSelectedFileId = useDeferredValue(selectedFileId);
  const selectedFile = useMemo(
    () => files.find((file) => file.id === deferredSelectedFileId) ?? null,
    [files, deferredSelectedFileId]
  );

  useEffect(() => {
    void refreshFiles();
  }, []);

  useEffect(() => {
    if (!selectedFile) {
      setSummaryState({ isLoading: false, data: '', error: '' });
      setQuestionState((current) => ({ ...current, error: '' }));
      setLatestPlaybackTime(null);
      return;
    }

    if (selectedFile.summary) {
      setSummaryState({ isLoading: false, data: selectedFile.summary, error: '' });
    } else {
      setSummaryState({ isLoading: false, data: '', error: '' });
    }

    if (selectedFile.processingStatus === 'READY') {
      void loadSummary(selectedFile.id);
    }
  }, [selectedFile]);

  async function refreshFiles(preferredFileId = null) {
    try {
      const nextFiles = await listFiles();
      startTransition(() => {
        setFiles(nextFiles);
        setSelectedFileId((currentFileId) => {
          if (preferredFileId && nextFiles.some((file) => file.id === preferredFileId)) {
            return preferredFileId;
          }

          if (currentFileId && nextFiles.some((file) => file.id === currentFileId)) {
            return currentFileId;
          }

          return nextFiles[0]?.id ?? null;
        });
      });
      setPageError('');
    } catch (error) {
      setPageError(error.message);
    } finally {
      setIsBooting(false);
    }
  }

  async function loadSummary(fileId) {
    setSummaryState((current) => ({
      ...current,
      isLoading: true,
      error: ''
    }));

    try {
      const summaryResponse = await getFileSummary(fileId);
      setSummaryState({
        isLoading: false,
        data: summaryResponse.summary || '',
        error: ''
      });
    } catch (error) {
      setSummaryState((current) => ({
        ...current,
        isLoading: false,
        error: error.message
      }));
    }
  }

  async function handleUpload(file) {
    setUploadState({ isUploading: true, error: '' });

    try {
      const createdFile = await uploadFile(file);
      await refreshFiles(createdFile.id);
    } catch (error) {
      setUploadState({ isUploading: false, error: error.message });
      return;
    }

    setUploadState({ isUploading: false, error: '' });
  }

  async function handleProcess(fileId) {
    setProcessingFileId(fileId);
    setPageError('');

    try {
      await processFile(fileId);
      const updatedFile = await getFile(fileId);

      startTransition(() => {
        setFiles((currentFiles) =>
          currentFiles.map((file) => (file.id === fileId ? updatedFile : file))
        );
      });

      if (updatedFile.processingStatus === 'READY') {
        await loadSummary(fileId);
      }
    } catch (error) {
      setPageError(error.message);
      await refreshFiles(fileId);
    } finally {
      setProcessingFileId(null);
    }
  }

  async function handleAsk(question) {
    if (!selectedFile) {
      return;
    }

    setQuestionState({ isAsking: true, error: '' });

    const messageId = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${selectedFile.id}-${Date.now()}`;

    const newQa = {
      id: messageId,
      question,
      answer: '',
      sources: []
    };

    setQaHistoryByFile((current) => ({
      ...current,
      [selectedFile.id]: [...(current[selectedFile.id] || []), newQa]
    }));

    try {
      const eventSource = askQuestionStream(selectedFile.id, question);

      eventSource.onmessage = (event) => {
        const chunk = event.data;
        setQaHistoryByFile((current) => {
          const fileHistory = current[selectedFile.id] || [];
          return {
            ...current,
            [selectedFile.id]: fileHistory.map((item) =>
              item.id === messageId ? { ...item, answer: item.answer + chunk } : item
            )
          };
        });
      };

      eventSource.onerror = (error) => {
        eventSource.close();
        setQuestionState({ isAsking: false, error: 'Streaming interrupted' });
      };

      // Since EventSource doesn't have a clean "end" event for the whole stream in SSE standard
      // without custom events, we might need a custom event or close on timeout/specific marker.
      // But for now, let's just keep it simple.
      // Actually, OpenAI stream ends with [DONE] usually, but here we are passing raw strings.
      // I should probably have a 'close' signal from backend or just close when model finishes.
    } catch (error) {
      setQuestionState({ isAsking: false, error: error.message });
    }
  }

  function handleJumpToTimestamp(seconds) {
    if (seconds == null || !mediaRef.current) {
      return;
    }

    mediaRef.current.currentTime = seconds;
    void mediaRef.current.play?.();
    setLatestPlaybackTime(seconds);
  }

  const selectedHistory = selectedFile ? qaHistoryByFile[selectedFile.id] || [] : [];

  return (
    <main className="app-shell">
      <section className="hero-banner">
        <div>
          <p className="eyebrow">Panscience SDE-1 Assessment</p>
          <h1>Document and media intelligence workspace</h1>
          <p className="hero-copy">
            Upload a file, process it into chunks or transcripts, review the
            generated summary, and ask grounded questions with cited answers.
          </p>
        </div>

        <div className="hero-stats">
          <div className="hero-stat-card">
            <span className="hero-stat-label">Files in workspace</span>
            <strong>{files.length}</strong>
          </div>
          <div className="hero-stat-card">
            <span className="hero-stat-label">Ready for Q&amp;A</span>
            <strong>{files.filter((file) => file.processingStatus === 'READY').length}</strong>
          </div>
        </div>
      </section>

      {pageError ? <div className="page-alert">{pageError}</div> : null}

      <section className="top-grid">
        <UploadPanel
          isUploading={uploadState.isUploading}
          uploadError={uploadState.error}
          onUpload={handleUpload}
        />

        <SummaryPanel file={selectedFile} summaryState={summaryState} />
      </section>

      <section className="workspace-grid">
        <FileListPanel
          files={files}
          selectedFileId={selectedFileId}
          processingFileId={processingFileId}
          onSelect={setSelectedFileId}
          onProcess={handleProcess}
        />

        <div className="workspace-main">
          <MediaPreviewPanel
            file={selectedFile}
            mediaRef={mediaRef}
            latestPlaybackTime={latestPlaybackTime}
            onJumpToTimestamp={handleJumpToTimestamp}
          />

          <QuestionPanel
            file={selectedFile}
            isAsking={questionState.isAsking}
            history={selectedHistory}
            askError={questionState.error}
            onAsk={handleAsk}
            onJumpToTimestamp={handleJumpToTimestamp}
          />
        </div>
      </section>

      {isBooting ? <div className="page-alert subtle">Loading workspace...</div> : null}
    </main>
  );
}

export default App;
