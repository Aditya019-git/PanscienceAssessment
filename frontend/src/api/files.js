const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, options);

  if (!response.ok) {
    let message = 'Request failed';

    try {
      const payload = await response.json();
      message = payload.message || message;
    } catch {
      try {
        message = await response.text();
      } catch {
        message = 'Request failed';
      }
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
}

export function listFiles() {
  return request('/files');
}

export function getFile(fileId) {
  return request(`/files/${fileId}`);
}

export function uploadFile(file) {
  const formData = new FormData();
  formData.append('file', file);

  return request('/files/upload', {
    method: 'POST',
    body: formData
  });
}

export function processFile(fileId) {
  return request(`/files/${fileId}/process`, {
    method: 'POST'
  });
}

export function getFileSummary(fileId) {
  return request(`/files/${fileId}/summary`);
}

export function askQuestion(fileId, question) {
  return request(`/files/${fileId}/questions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ question })
  });
}

export function askQuestionStream(fileId, question) {
  const url = new URL(`${API_BASE}/files/${fileId}/questions/stream`);
  url.searchParams.append('question', question);
  return new EventSource(url.toString());
}

export function buildFileContentUrl(fileId) {
  return `${API_BASE}/files/${fileId}/content`;
}
