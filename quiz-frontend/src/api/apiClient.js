const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

async function request(path, options = {}) {
  const token = localStorage.getItem("quizora_token");
  const headers = {
    ...(options.body && !(options.body instanceof FormData) ? { "Content-Type": "application/json" } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), Number(import.meta.env.VITE_API_TIMEOUT_MS || 20000));
  let response;
  try { response = await fetch(API_URL + path, { ...options, headers, signal: controller.signal }); }
  catch (e) { if (e?.name === "AbortError") { const err = new Error("The server took too long to respond."); err.status = 408; throw err; } throw e; }
  finally { clearTimeout(timeout); }
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }

  if (!response.ok) {
    const message = typeof data === "string" ? data : data?.message || `Request failed (${response.status})`;
    const error = new Error(message);
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}

export const api = {
  login: (p) => request("/auth/login", { method: "POST", body: JSON.stringify(p) }),
  registerParticipant: (p) => request("/auth/register/participant", { method: "POST", body: JSON.stringify(p) }),
  registerHost: (p) => request("/auth/register/host", { method: "POST", body: JSON.stringify(p) }),
  me: () => request("/auth/me"),
  updateProfile: (p) => request("/auth/profile", { method: "PATCH", body: JSON.stringify(p) }),
  hostDashboard: () => request("/host/dashboard"),
  createQuiz: (p) => request("/host/quizzes", { method: "POST", body: JSON.stringify(p) }),
  myQuizzes: () => request("/host/quizzes"),
  publishQuiz: (id) => request(`/host/quizzes/${id}/publish`, { method: "PATCH" }),
  startQuiz: (id) => request(`/host/quizzes/${id}/start`, { method: "PATCH" }),
  getHostQuiz: (id) => request(`/host/quizzes/${id}`),
  updateQuiz: (id, p) => request(`/host/quizzes/${id}`, { method: "PUT", body: JSON.stringify(p) }),
  deleteQuiz: (id) => request(`/host/quizzes/${id}`, { method: "DELETE" }),
  closeQuiz: (id) => request(`/host/quizzes/${id}/close`, { method: "PATCH" }),
  reopenQuiz: (id) => request(`/host/quizzes/${id}/reopen`, { method: "PATCH" }),
  updateQuizPublication: (id, p) => request(`/host/quizzes/${id}/publication`, { method: "PATCH", body: JSON.stringify(p) }),
  publicQuiz: (id) => request(`/public/quizzes/${id}`),
  joinedQuizzes: () => request("/participant/quizzes/joined"),
  joinQuiz: (id) => request(`/participant/quizzes/${id}/join`, { method: "POST" }),
  joinStatus: (id) => request(`/participant/quizzes/${id}/status`),
  publicQuizzes: (params = {}) => { const q = new URLSearchParams(Object.entries(params).filter(([,v]) => v !== undefined && v !== null && v !== "")); return request(`/public/quizzes${q.toString() ? `?${q}` : ""}`); },
  publicQuizResults: (id) => request(`/public/quizzes/${id}/results`),
  startAttempt: (id) => request(`/participant/attempts/${id}/start`, { method: "POST" }),
  getAttempt: (id) => request(`/participant/attempts/${id}`),
  submitAttempt: (id, answers, questionTimeSeconds) => request(`/participant/attempts/${id}/submit`, { method: "POST", body: JSON.stringify({ answers, questionTimeSeconds }) }),
  saveAttempt: (id, answers, questionTimeSeconds) => request(`/participant/attempts/${id}/save`, { method: "PATCH", body: JSON.stringify({ answers, questionTimeSeconds }) }),
  recordAttemptActivity: (id, type) => request(`/participant/attempts/${id}/activity`, { method: "POST", body: JSON.stringify({ type }) }),
  leaderboard: (id) => request(`/participant/attempts/${id}/leaderboard`),
  hostQuizResults: (id) => request(`/host/quizzes/${id}/results`),
  hostQuizMonitor: (id) => request(`/host/quizzes/${id}/monitor`),
  hostQuizParticipants: (id) => request(`/host/quizzes/${id}/participants`),
  hostAttemptDetails: (quizId, attemptId) => request(`/host/quizzes/${quizId}/attempts/${attemptId}`),
  hostQuestionAnalytics: (id) => request(`/host/quizzes/${id}/question-analytics`),
  exportHostResults: async (id) => {
    const token = localStorage.getItem("quizora_token");
    const response = await fetch(`${API_URL}/host/quizzes/${id}/results/export`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
    if (!response.ok) { const text = await response.text(); throw new Error(text || `Request failed (${response.status})`); }
    return response.blob();
  },
  hostQuizAnalytics: (id) => request(`/host/quizzes/${id}/analytics`),
  participantHistory: () => request("/participant/history"),
  participantProfile: () => request("/participant/profile"),
  pendingHosts: () => request("/admin/hosts/pending"),
  allHosts: () => request("/admin/hosts"),
  setHostQuizPosting: (id, enabled) => request(`/admin/hosts/${id}/quiz-posting`, { method: "PATCH", body: JSON.stringify({ enabled }) }),
  approveHost: (id) => request(`/admin/hosts/${id}/approve`, { method: "PATCH" }),
  rejectHost: (id, reason) => request(`/admin/hosts/${id}/reject`, { method: "PATCH", body: JSON.stringify({ reason }) }),
  allUsers: () => request("/admin/users"),
  getAdminUser: (id) => request(`/admin/users/${id}`),
  updateAdminUser: (id, p) => request(`/admin/users/${id}`, { method: "PATCH", body: JSON.stringify(p) }),
  setUserStatus: (id, status) => request(`/admin/users/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
  deleteAdminUser: (id) => request(`/admin/users/${id}`, { method: "DELETE" }),
  allAdminQuizzes: () => request("/admin/quizzes"),
  adminAnalytics: () => request("/admin/analytics"),
  adminAnalyticsRange: (from,to) => request(`/admin/analytics/range?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`),
  adminAudit: (params={}) => { const q=new URLSearchParams(Object.entries(params).filter(([,v])=>v)); return request(`/admin/audit${q.toString()?`?${q}`:""}`); },
  exportAdminAudit: async (params={}) => { const q=new URLSearchParams(Object.entries(params).filter(([,v])=>v)); const token=localStorage.getItem("quizora_token"); const r=await fetch(`${API_URL}/admin/audit/export${q.toString()?`?${q}`:""}`,{headers:token?{Authorization:`Bearer ${token}`}:{}}); if(!r.ok) throw new Error(await r.text()||`Request failed (${r.status})`); return r.blob(); },
  exportAdminAnalytics: async () => {
    const token = localStorage.getItem("quizora_token");
    const response = await fetch(`${API_URL}/admin/analytics/export`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
    if (!response.ok) { const text = await response.text(); throw new Error(text || `Request failed (${response.status})`); }
    return response.blob();
  },
  getAdminQuiz: (id) => request(`/admin/quizzes/${id}`),
  updateAdminQuiz: (id, p) => request(`/admin/quizzes/${id}`, { method: "PUT", body: JSON.stringify(p) }),
  deleteAdminQuiz: (id) => request(`/admin/quizzes/${id}`, { method: "DELETE" }),
  suspendAdminQuiz: (id) => request(`/admin/quizzes/${id}/suspend`, { method: "PATCH" }),
  resumeAdminQuiz: (id) => request(`/admin/quizzes/${id}/resume`, { method: "PATCH" }),
  questionBank: () => request("/host/question-bank"),
  saveQuestionBankItem: (p) => request("/host/question-bank", { method: "POST", body: JSON.stringify(p) }),
  deleteQuestionBankItem: (id) => request(`/host/question-bank/${id}`, { method: "DELETE" }),
  importQuestionBank: (file) => { const form = new FormData(); form.append("file", file); return request("/host/question-bank/import", { method: "POST", body: form }); },
  notifications: () => request("/notifications"),
  unreadNotificationCount: () => request("/notifications/unread-count"),
  markNotificationRead: (id) => request(`/notifications/${id}/read`, { method: "PATCH" }),
  markAllNotificationsRead: () => request("/notifications/read-all", { method: "PATCH" }),
  exportQuestionBank: async () => {
    const token = localStorage.getItem("quizora_token");
    const response = await fetch(`${API_URL}/host/question-bank/export`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
    if (!response.ok) { const text = await response.text(); throw new Error(text || `Request failed (${response.status})`); }
    return response.blob();
  }
};
