const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

async function request(path, options = {}, token = null) {
  const headers = { ...options.headers };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  if (!response.ok) {
    let message = 'Request failed';

    try {
      const text = await response.text();
      if (text) {
        const payload = JSON.parse(text);
        message = payload.message || message;
      }
    } catch {
      message = 'Request failed';
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  }

  return response.text();
}

export function listFiles(token) {
  return request('/files', {}, token);
}

export function getFile(fileId, token) {
  return request(`/files/${fileId}`, {}, token);
}

export function uploadFile(file, token) {
  const formData = new FormData();
  formData.append('file', file);

  return request('/files/upload', {
    method: 'POST',
    body: formData
  }, token);
}

export function processFile(fileId, token) {
  return request(`/files/${fileId}/process`, {
    method: 'POST'
  }, token);
}

export function getFileSummary(fileId, token) {
  return request(`/files/${fileId}/summary`, {}, token);
}

export function askQuestion(fileId, question, token) {
  return request(`/files/${fileId}/questions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ question })
  }, token);
}

export function askQuestionStream(fileId, question, token) {
  const url = new URL(`${API_BASE}/files/${fileId}/questions/stream`);
  url.searchParams.append('question', question);
  url.searchParams.append('token', token);
  return new EventSource(url.toString());
}

export function buildFileContentUrl(fileId) {
  return `${API_BASE}/files/${fileId}/content`;
}
