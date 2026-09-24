import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { api } from "../api/apiClient";

export default function HostDashboard() {
  const { user, updateUser } = useAuth();
  const [approved, setApproved] = useState(user?.hostApprovalStatus === "APPROVED");
  const [postingEnabled, setPostingEnabled] = useState(true);
  const [message, setMessage] = useState("Checking approval status…");
  const [quizzes, setQuizzes] = useState([]);
  const [busyId, setBusyId] = useState("");

  async function loadQuizzes() {
    try { const d = await api.myQuizzes(); setQuizzes(Array.isArray(d) ? d : []); }
    catch (e) { setMessage(e.message || "Unable to load quizzes."); }
  }

  useEffect(() => {
    let active = true;
    api.hostDashboard().then(d => {
      if (!active) return;
      setApproved(true); setPostingEnabled(d?.quizPostingEnabled !== false); setMessage(d?.message || "Approved host access granted");
      updateUser({ hostApprovalStatus: "APPROVED" });
      loadQuizzes();
    }).catch(e => {
      if (active) { setApproved(false); if (e.status === 403) setMessage("Your host account is still awaiting administrator approval."); else setMessage(e.message || "Unable to verify host approval."); }
    });
    return () => { active = false; };
  }, []);

  async function start(id) {
    setBusyId(id);
    try { await api.startQuiz(id); await loadQuizzes(); }
    catch (e) { setMessage(e.message || "Unable to start competition."); }
    finally { setBusyId(""); }
  }

  async function publish(id) {
    setBusyId(id);
    try { await api.publishQuiz(id); await loadQuizzes(); }
    catch (e) { setMessage(e.message || "Unable to publish quiz."); }
    finally { setBusyId(""); }
  }

  async function remove(id) {
    if (!window.confirm("Delete this quiz permanently? This action cannot be undone.")) return;
    setBusyId(id);
    try { await api.deleteQuiz(id); await loadQuizzes(); }
    catch (e) { setMessage(e.message || "Unable to delete quiz."); }
    finally { setBusyId(""); }
  }

  return <>
    <div className="page-heading"><div><span className="eyebrow">HOST CENTRE</span><h1>Host dashboard</h1><p>Create, review, publish and start your quiz competitions.</p></div>{approved && postingEnabled && <Link className="button-primary small" to="/host/create">Create quiz <span>＋</span></Link>}</div>
    <div className="template-download-card"><div><strong>Question Bank CSV template</strong><p>Download the sample CSV, add your questions, then import it from Question Bank.</p></div><a className="button-ghost dark small" href="/QUESTION_BANK_TEMPLATE.csv" download="QUESTION_BANK_TEMPLATE.csv">Download template ↓</a></div>
    <div className={`approval-banner ${approved ? "approved" : "pending"}`}><div className="approval-symbol">{approved ? "✓" : "⏳"}</div><div><strong>{approved ? "Host account approved" : "Approval pending"}</strong><p>{approved ? (postingEnabled ? "You can now create and manage competitions." : "Your administrator has disabled new quiz posting. You can still manage your existing competitions.") : "An administrator must approve your host account before host-management features become available."}</p></div><span className="status-pill">{approved ? (postingEnabled ? "APPROVED" : "POSTING DISABLED") : "PENDING"}</span></div>
    {!approved ? <div className="empty-panel"><div className="empty-icon">♙</div><h2>Thanks for registering as a host</h2><p>{message}</p></div> : <div className="table-panel"><div className="table-head"><div><h2>My quizzes</h2><p>Open any quiz to review all question details, edit it, publish it or delete it.</p></div>{postingEnabled && <Link className="button-ghost dark small" to="/host/create">+ Create quiz</Link>}</div>{!quizzes.length ? <div className="table-empty"><div className="empty-icon">✦</div><h3>No quizzes yet</h3><p>Create your first competition and add MCQ, True/False or Correct Word questions.</p>{postingEnabled ? <Link className="button-primary" to="/host/create">Create your first quiz</Link> : <span className="status-pill pending">New quiz posting disabled by admin</span>}</div> : <div className="host-list">{quizzes.map(q => <div className="host-row" key={q.id}><div className="host-info"><strong>{q.title}</strong><span>{q.description || "No description"} · {q.questions?.length || 0} questions</span></div><div className="host-row-actions"><span className={`status-pill ${q.status === "PUBLISHED" ? "approved" : "pending"}`}>{q.status}</span><Link className="button-ghost dark small" to={`/host/quizzes/${q.id}`}>View</Link><Link className="button-ghost dark small" to={`/host/quizzes/${q.id}/results`}>Results</Link><Link className="button-ghost dark small" to={`/host/quizzes/${q.id}/edit`}>Edit</Link>{q.status === "DRAFT" && <button className="button-primary small" disabled={busyId === q.id} onClick={() => publish(q.id)}>{busyId === q.id ? "Working…" : "Publish"}</button>}{q.status === "PUBLISHED" && <button className="button-primary small" disabled={busyId === q.id} onClick={() => start(q.id)}>{busyId === q.id ? "Starting…" : "Start"}</button>}<button className="button-danger small" disabled={busyId === q.id} onClick={() => remove(q.id)}>Delete</button></div></div>)}</div>}</div>}
  </>;
}
