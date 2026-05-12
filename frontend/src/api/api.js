const API_BASE = '/api';

export const login = async (email, password) => {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) throw new Error('Login failed');
  return response.json();
};

export const register = async (email, password) => {
  const response = await fetch(`${API_BASE}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) throw new Error('Registration failed');
  return response.json();
};

export const uploadFile = async (file, token) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_BASE}/files/upload`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData,
  });
  if (!response.ok) throw new Error('Upload failed');
  return response.json();
};

export const getFiles = async (token) => {
  const response = await fetch(`${API_BASE}/files`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });
  if (!response.ok) throw new Error('Failed to fetch files');
  return response.json();
};

export const processFile = async (fileId, token) => {
  const response = await fetch(`${API_BASE}/files/${fileId}/process`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
  });
  if (!response.ok) throw new Error('Processing failed');
  return response.json();
};

export const getSummary = async (fileId, token) => {
  const response = await fetch(`${API_BASE}/files/${fileId}/summary`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });
  if (!response.ok) throw new Error('Failed to fetch summary');
  return response.json();
};

export const askQuestion = async (fileId, question, token) => {
  const response = await fetch(`${API_BASE}/files/${fileId}/questions`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ question }),
  });
  if (!response.ok) throw new Error('Failed to get answer');
  return response.json();
};
