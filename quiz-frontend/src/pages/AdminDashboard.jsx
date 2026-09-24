import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/apiClient";

function fmtDate(value) {
  if (!value) return "—";
  try { return new Date(value).toLocaleString(); } catch { return value; }
}

function Status({ children, tone = "approved" }) {
  return <span className={`status-pill ${tone}`}>{children}</span>;
}

export default function AdminDashboard() {
  const [tab, setTab] = useState("overview");
  const [users, setUsers] = useState([]);
  const [hosts, setHosts] = useState([]);
  const [quizzes, setQuizzes] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [selectedQuiz, setSelectedQuiz] = useState(null);
  const [editUser, setEditUser] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");

  async function loadAll() {
    try {
      setError("");
      const [u, h, q] = await Promise.all([api.allUsers(), api.allHosts(), api.allAdminQuizzes()]);
      setUsers(u || []); setHosts(h || []); setQuizzes(q || []);
    } catch (e) { setError(e.message || "Unable to load admin data."); }
  }
  useEffect(() => { loadAll(); }, []);

  const hostMap = useMemo(() => Object.fromEntries(users.map(u => [u.id, u])), [users]);
  const participants = users.filter(u => u.role === "PARTICIPANT");
  const adminUsers = users.filter(u => u.role === "ADMIN");
  const suspendedUsers = users.filter(u => u.accountStatus === "SUSPENDED");
  const pendingHosts = hosts.filter(h => h.hostApprovalStatus === "PENDING");
  const activeQuizzes = quizzes.filter(q => q.status === "PUBLISHED" || q.status === "STARTED");
  const suspendedQuizzes = quizzes.filter(q => q.status === "SUSPENDED");

  async function action(key, fn) {
    setBusy(key); setError("");
    try { await fn(); await loadAll(); }
    catch (e) { setError(e.message || "Operation failed."); }
    finally { setBusy(""); }
  }

  async function saveUser(e) {
    e.preventDefault();
    await action(`edit-user-${editUser.id}`, async () => {
      await api.updateAdminUser(editUser.id, { name: editUser.name, email: editUser.email });
      setEditUser(null);
    });
  }

  async function removeUser(user) {
    if (!window.confirm(`Delete ${user.name || user.email}? This cannot be undone.`)) return;
    await action(`delete-user-${user.id}`, async () => api.deleteAdminUser(user.id));
  }

  async function changeUserStatus(user) {
    const next = user.accountStatus === "SUSPENDED" ? "ACTIVE" : "SUSPENDED";
    if (!window.confirm(`${next === "SUSPENDED" ? "Suspend" : "Reactivate"} ${user.name || user.email}?`)) return;
    await action(`status-user-${user.id}`, async () => api.setUserStatus(user.id, next));
  }

  async function hostDecision(host, approved) {
    await action(`host-${host.id}-${approved ? "approve" : "reject"}`, async () => {
      if (approved) await api.approveHost(host.id);
      else await api.rejectHost(host.id, window.prompt("Rejection reason (optional):") || "");
    });
  }

  async function togglePosting(host) {
    await action(`posting-${host.id}`, () => api.setHostQuizPosting(host.id, !host.quizPostingEnabled));
  }

  async function deleteQuiz(q) {
    if (!window.confirm(`Delete quiz “${q.title}”? All join records for this quiz will also be removed.`)) return;
    await action(`delete-quiz-${q.id}`, async () => api.deleteAdminQuiz(q.id));
  }

  async function toggleQuizSuspension(q) {
    const suspend = q.status !== "SUSPENDED";
    if (!window.confirm(`${suspend ? "Suspend" : "Resume"} quiz “${q.title}”?`)) return;
    await action(`suspend-quiz-${q.id}`, () => suspend ? api.suspendAdminQuiz(q.id) : api.resumeAdminQuiz(q.id));
  }

  const tabs = [
    ["overview", "Overview"], ["users", "All Users"], ["hosts", "Hosts"], ["quizzes", "Quizzes"]
  ];

  return <>
    <div className="page-heading">
      <div><span className="eyebrow">ADMIN CONSOLE</span><h1>Platform management</h1><p>Manage users, hosts and every quiz from one place.</p></div>
      <div className="form-actions"><Link className="button-ghost dark small" to="/admin/analytics">Analytics</Link><Link className="button-ghost dark small" to="/admin/audit">Audit history</Link><button className="button-ghost dark small" onClick={loadAll}>↻ Refresh</button></div>
    </div>

    {error && <div className="alert error">{error}</div>}

    <div className="admin-tabs">
      {tabs.map(([id, label]) => <button key={id} className={tab === id ? "active" : ""} onClick={() => setTab(id)}>{label}</button>)}
    </div>

    <div className="template-download-card"><div><strong>Question Bank CSV template</strong><p>Sample format for MCQ, True/False and Correct Word questions. Admins can keep this as the standard import template for hosts.</p></div><a className="button-ghost dark small" href="/QUESTION_BANK_TEMPLATE.csv" download="QUESTION_BANK_TEMPLATE.csv">Download template ↓</a></div>
    {tab === "overview" && <>
      <div className="admin-stat-grid">
        <div className="admin-stat"><span>Total users</span><strong>{users.length}</strong><small>{participants.length} participants</small></div>
        <div className="admin-stat"><span>Hosts</span><strong>{hosts.length}</strong><small>{pendingHosts.length} pending approval</small></div>
        <div className="admin-stat"><span>Quizzes</span><strong>{quizzes.length}</strong><small>{activeQuizzes.length} published/live</small></div>
        <div className="admin-stat"><span>Suspended</span><strong>{suspendedUsers.length + suspendedQuizzes.length}</strong><small>{suspendedUsers.length} users · {suspendedQuizzes.length} quizzes</small></div>
      </div>
      <div className="admin-overview-grid">
        <section className="table-panel"><div className="table-head"><div><h2>Pending host approvals</h2><p>Review host registration requests.</p></div><button className="button-ghost dark small" onClick={() => setTab("hosts")}>Manage hosts</button></div>
          {pendingHosts.slice(0, 5).map(h => <div className="host-row" key={h.id}><div className="avatar">{(h.name || "H")[0].toUpperCase()}</div><div className="host-info"><strong>{h.name}</strong><span>{h.email}</span></div><Status tone="pending">PENDING</Status><div className="row-actions"><button className="approve" onClick={() => hostDecision(h, true)}>Approve</button><button className="reject" onClick={() => hostDecision(h, false)}>Reject</button></div></div>)}
          {!pendingHosts.length && <div className="table-empty"><h3>No pending hosts</h3><p>All host registrations have been reviewed.</p></div>}
        </section>
        <section className="table-panel"><div className="table-head"><div><h2>Recent quizzes</h2><p>Quick access to platform-wide quiz controls.</p></div><button className="button-ghost dark small" onClick={() => setTab("quizzes")}>Manage quizzes</button></div>
          {quizzes.slice(0, 5).map(q => <div className="admin-list-row" key={q.id}><div><strong>{q.title}</strong><span>{hostMap[q.hostId]?.name || q.hostId || "System"} · {q.questions?.length || 0} questions</span></div><Status tone={q.status === "SUSPENDED" ? "pending" : q.status === "STARTED" ? "approved" : "neutral"}>{q.status}</Status><button className="button-ghost dark small" onClick={() => setSelectedQuiz(q)}>View</button></div>)}
          {!quizzes.length && <div className="table-empty"><h3>No quizzes</h3><p>There are no quizzes in the database.</p></div>}
        </section>
      </div>
    </>}

    {tab === "users" && <section className="table-panel"><div className="table-head"><div><h2>All users</h2><p>View, edit, suspend, reactivate or delete participant and host accounts.</p></div><span className="count-chip">{users.length} users</span></div>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>User</th><th>Role</th><th>Account</th><th>Host approval</th><th>Created</th><th>Actions</th></tr></thead><tbody>
        {users.map(u => <tr key={u.id}><td><strong>{u.name || "—"}</strong><small>{u.email}</small></td><td><Status tone={u.role === "HOST" ? "pending" : u.role === "ADMIN" ? "neutral" : "approved"}>{u.role}</Status></td><td><Status tone={u.accountStatus === "SUSPENDED" ? "pending" : "approved"}>{u.accountStatus || "ACTIVE"}</Status></td><td>{u.role === "HOST" ? <Status tone={u.hostApprovalStatus === "APPROVED" ? "approved" : "pending"}>{u.hostApprovalStatus || "PENDING"}</Status> : "—"}</td><td>{fmtDate(u.createdAt)}</td><td><div className="admin-actions"><button onClick={() => setSelectedUser(u)}>View</button>{u.role !== "ADMIN" && <><button onClick={() => setEditUser({...u})}>Edit</button><button onClick={() => changeUserStatus(u)}>{u.accountStatus === "SUSPENDED" ? "Reactivate" : "Suspend"}</button><button className="danger" onClick={() => removeUser(u)}>Delete</button></>}</div></td></tr>)}
      </tbody></table></div>
    </section>}

    {tab === "hosts" && <section className="table-panel"><div className="table-head"><div><h2>Host management</h2><p>Approve/reject hosts, suspend accounts and control new quiz posting.</p></div><span className="count-chip">{hosts.length} hosts</span></div>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Host</th><th>Approval</th><th>Account</th><th>Quiz posting</th><th>Created</th><th>Actions</th></tr></thead><tbody>
        {hosts.map(h => <tr key={h.id}><td><strong>{h.name || "—"}</strong><small>{h.email}</small></td><td><Status tone={h.hostApprovalStatus === "APPROVED" ? "approved" : "pending"}>{h.hostApprovalStatus || "PENDING"}</Status></td><td><Status tone={h.accountStatus === "SUSPENDED" ? "pending" : "approved"}>{h.accountStatus || "ACTIVE"}</Status></td><td><Status tone={h.quizPostingEnabled === false ? "pending" : "approved"}>{h.quizPostingEnabled === false ? "DISABLED" : "ENABLED"}</Status></td><td>{fmtDate(h.createdAt)}</td><td><div className="admin-actions"><button onClick={() => setSelectedUser(h)}>View</button><button onClick={() => setEditUser({...h})}>Edit</button>{h.hostApprovalStatus === "PENDING" ? <><button onClick={() => hostDecision(h, true)}>Approve</button><button className="danger" onClick={() => hostDecision(h, false)}>Reject</button></> : <button onClick={() => togglePosting(h)}>{h.quizPostingEnabled === false ? "Enable posting" : "Disable posting"}</button>}<button onClick={() => changeUserStatus(h)}>{h.accountStatus === "SUSPENDED" ? "Reactivate" : "Suspend"}</button><button className="danger" onClick={() => removeUser(h)}>Delete</button></div></td></tr>)}
      </tbody></table></div>
    </section>}

    {tab === "quizzes" && <section className="table-panel"><div className="table-head"><div><h2>All quizzes</h2><p>View full quiz details, edit questions, suspend/resume or delete competitions.</p></div><span className="count-chip">{quizzes.length} quizzes</span></div>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Quiz</th><th>Host</th><th>Questions</th><th>Duration</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead><tbody>
        {quizzes.map(q => <tr key={q.id}><td><strong>{q.title}</strong><small>{q.description || "No description"}</small></td><td>{hostMap[q.hostId]?.name || "System"}<small>{hostMap[q.hostId]?.email || q.hostId || "—"}</small></td><td>{q.questions?.length || 0}</td><td>{q.durationMinutes} min</td><td><Status tone={q.status === "SUSPENDED" ? "pending" : q.status === "STARTED" ? "approved" : "neutral"}>{q.status}</Status></td><td>{fmtDate(q.createdAt)}</td><td><div className="admin-actions"><button onClick={() => setSelectedQuiz(q)}>View</button><Link className="admin-button-link" to={`/admin/quizzes/${q.id}/edit`}>Edit</Link><button onClick={() => toggleQuizSuspension(q)}>{q.status === "SUSPENDED" ? "Resume" : "Suspend"}</button><button className="danger" onClick={() => deleteQuiz(q)}>Delete</button></div></td></tr>)}
      </tbody></table></div>
    </section>}

    {selectedUser && <div className="modal-backdrop" onMouseDown={e => e.target === e.currentTarget && setSelectedUser(null)}><div className="modal-card admin-modal"><div className="modal-head"><div><span className="eyebrow">USER DETAILS</span><h2>{selectedUser.name || "User"}</h2><p>{selectedUser.email}</p></div><button className="modal-close" onClick={() => setSelectedUser(null)}>×</button></div><div className="detail-grid"><div><span>Role</span><strong>{selectedUser.role}</strong></div><div><span>Account</span><strong>{selectedUser.accountStatus || "ACTIVE"}</strong></div><div><span>Created</span><strong>{fmtDate(selectedUser.createdAt)}</strong></div><div><span>Host approval</span><strong>{selectedUser.hostApprovalStatus || "—"}</strong></div><div><span>Quiz posting</span><strong>{selectedUser.role === "HOST" ? (selectedUser.quizPostingEnabled === false ? "Disabled" : "Enabled") : "—"}</strong></div><div><span>ID</span><strong className="break-id">{selectedUser.id}</strong></div></div>{selectedUser.rejectionReason && <div className="alert error">Rejection reason: {selectedUser.rejectionReason}</div>}<div className="form-actions"><button className="button-ghost dark" onClick={() => {setEditUser({...selectedUser});setSelectedUser(null)}}>Edit</button><button className="button-primary" onClick={() => setSelectedUser(null)}>Close</button></div></div></div>}

    {editUser && <div className="modal-backdrop" onMouseDown={e => e.target === e.currentTarget && setEditUser(null)}><form className="modal-card admin-modal" onSubmit={saveUser}><div className="modal-head"><div><span className="eyebrow">EDIT USER</span><h2>Update account</h2><p>Change the user's basic account details.</p></div><button type="button" className="modal-close" onClick={() => setEditUser(null)}>×</button></div><label>Name<input value={editUser.name || ""} onChange={e => setEditUser({...editUser,name:e.target.value})} required /></label><label>Email<input type="email" value={editUser.email || ""} onChange={e => setEditUser({...editUser,email:e.target.value})} required /></label><div className="form-actions"><button type="button" className="button-ghost dark" onClick={() => setEditUser(null)}>Cancel</button><button className="button-primary" disabled={busy === `edit-user-${editUser.id}`}>{busy === `edit-user-${editUser.id}` ? "Saving…" : "Save changes"}</button></div></form></div>}

    {selectedQuiz && <div className="modal-backdrop" onMouseDown={e => e.target === e.currentTarget && setSelectedQuiz(null)}><div className="modal-card admin-modal admin-quiz-modal"><div className="modal-head"><div><span className="eyebrow">QUIZ DETAILS</span><h2>{selectedQuiz.title}</h2><p>{selectedQuiz.description || "No description"}</p></div><button className="modal-close" onClick={() => setSelectedQuiz(null)}>×</button></div><div className="detail-grid"><div><span>Host</span><strong>{hostMap[selectedQuiz.hostId]?.name || "System"}</strong></div><div><span>Status</span><strong>{selectedQuiz.status}</strong></div><div><span>Questions</span><strong>{selectedQuiz.questions?.length || 0}</strong></div><div><span>Duration</span><strong>{selectedQuiz.durationMinutes} minutes</strong></div><div><span>Scheduled start</span><strong>{fmtDate(selectedQuiz.scheduledStartAt)}</strong></div><div><span>Scheduled end</span><strong>{fmtDate(selectedQuiz.scheduledEndAt)}</strong></div></div><div className="admin-question-list"><h3>Questions</h3>{(selectedQuiz.questions || []).map((q,i)=><div className="admin-question" key={i}><strong>Q{i+1}. {q.question}</strong><span>{q.type} · Correct answer: {q.correctAnswer}</span><div>{(q.options || []).map((o,j)=><small key={j}>{String.fromCharCode(65+j)}. {o}</small>)}</div></div>)}</div><div className="form-actions"><Link className="button-ghost dark" to={`/admin/quizzes/${selectedQuiz.id}/edit`}>Edit full quiz</Link><button className="button-primary" onClick={() => setSelectedQuiz(null)}>Close</button></div></div></div>}
  </>;
}
