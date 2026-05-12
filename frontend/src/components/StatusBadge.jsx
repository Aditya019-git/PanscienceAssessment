const STATUS_COPY = {
  UPLOADED: 'Uploaded',
  PROCESSING: 'Processing',
  READY: 'Ready',
  FAILED: 'Failed'
};

export function StatusBadge({ status }) {
  if (!status) {
    return null;
  }

  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      {STATUS_COPY[status] || status}
    </span>
  );
}
