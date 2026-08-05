import api from './api';

export const getTransactions = (params) => api.get('/transactions', { params });
export const getTransaction = (id) => api.get(`/transactions/${id}`);
export const createTransaction = (data) => api.post('/transactions', data);
export const generateTransactions = (count) => api.post('/transactions/generate', { count });
