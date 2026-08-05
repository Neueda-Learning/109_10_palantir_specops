import api from './api';

export const getRules = () => api.get('/rules');
export const getRule = (id) => api.get(`/rules/${id}`);
export const createRule = (data) => api.post('/rules', data);
export const updateRule = (id, data) => api.put(`/rules/${id}`, data);
export const deleteRule = (id) => api.delete(`/rules/${id}`);
export const activateRule = (id) => api.patch(`/rules/${id}/activate`);
export const deactivateRule = (id) => api.patch(`/rules/${id}/deactivate`);
