import api from './api';

export const getAlerts = (params) => api.get('/alerts', { params });
export const getAlert = (id) => api.get(`/alerts/${id}`);
export const getAlertStats = () => api.get('/alerts/stats');
export const acknowledgeAlert = (id) => api.patch(`/alerts/${id}/acknowledge`);
export const investigateAlert = (id) => api.patch(`/alerts/${id}/investigate`);
export const closeAlert = (id, resolutionNotes) => api.patch(`/alerts/${id}/close`, { resolutionNotes });
export const dismissAlert = (id, resolutionNotes) => api.patch(`/alerts/${id}/dismiss`, { resolutionNotes });
