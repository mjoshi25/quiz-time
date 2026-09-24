import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { api } from "../api/apiClient";

export default function ParticipantDashboard() {
  const { user } = useAuth();
  const [joined, setJoined] = useState([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { api.joinedQuizzes().then(d => setJoined(Array.isArray(d) ? d : [])).catch(() => {}).finally(() => setLoading(false)); }, []);
  return <>
    <div className="page-heading"><div><span className="eyebrow">PARTICIPANT WORKSPACE</span><h1>Hello, {user?.name?.split(" ")[0] || "Participant"} 👋</h1><p>Join competitions and keep track of the quizzes you have selected.</p></div><Link className="button-primary small" to="/quizzes">Explore quizzes <span>→</span></Link></div>
    <div className="stats-grid"><div className="stat-card"><span>QUIZZES JOINED</span><strong>{joined.length}</strong><small>Competitions in your workspace</small></div><div className="stat-card"><span>YOUR POINTS</span><strong>0</strong><small>Points appear after attempts</small></div><div className="stat-card"><span>RANKING</span><strong>—</strong><small>Compete to get ranked</small></div></div>
    <div className="table-panel"><div className="table-head"><div><h2>My joined quizzes</h2><p>These are the published competitions you have joined.</p></div><Link className="button-ghost dark small" to="/quizzes">Explore more</Link></div>{loading ? <div className="table-empty"><h3>Loading…</h3></div> : !joined.length ? <div className="table-empty"><div className="empty-icon">✦</div><h3>No joined quizzes yet</h3><p>Explore published competitions and click Join quiz to add one here.</p><Link className="button-primary" to="/quizzes">Explore quizzes</Link></div> : <div className="host-list">{joined.map(q => <div className="host-row" key={q.id}><div className="host-info"><strong>{q.title}</strong><span>{q.description || "No description"} · {q.questionCount} questions</span></div><div className="host-row-actions"><span className="status-pill approved">JOINED</span><Link className="button-ghost dark small" to={`/quiz/${q.id}/start`}>Start quiz →</Link></div></div>)}</div>}</div>
  </>;
}
