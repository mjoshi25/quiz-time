import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../api/apiClient";

function timeSince(value) {
  if (!value) return "—";
  const sec = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000));
  if (sec < 60) return `${sec}s ago`;
  const min = Math.floor(sec / 60);
  return `${min}m ${sec % 60}s ago`;
}

function statusClass(status) {
  if (status === "IN_PROGRESS") return "approved";
  if (status === "SUBMITTED") return "approved";
  return "pending";
}

export default function HostQuizMonitorPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  async function load(silent = false) {
    if (!silent) setError("");
    try { setData(await api.hostQuizMonitor(id)); }
    catch (e) { setError(e.message || "Unable to load live competition monitor."); }
  }

  useEffect(() => {
    load();
    const timer = setInterval(() => load(true), 3000);
    return () => clearInterval(timer);
  }, [id]);

  if (!data && !error) return <div className="empty-panel"><h2>Loading live monitor…</h2></div>;
  if (error && !data) return <div className="alert error">{error}</div>;

  const events = data.activityTotals || {};
  return <div className="dashboard-form-page">
    <div className="form-page-top">
      <div>
        <span className="eyebrow">LIVE COMPETITION MONITOR</span>
        <h1>{data.title}</h1>
        <p>Automatically refreshed every 3 seconds. Activity events are informational browser signals, not proof of misconduct.</p>
      </div>
      <div className="form-actions">
        <Link className="button-ghost dark" to={`/host/quizzes/${id}/results`}>← Results</Link>
        <Link className="button-ghost dark" to={`/host/quizzes/${id}`}>Quiz details</Link>
        <button className="button-primary small" onClick={() => navigate(`/host/quizzes/${id}/analytics`)}>Analytics</button>
      </div>
    </div>

    {error && <div className="alert error">{error}</div>}

    <div className="details-panel results-stats monitor-stats">
      <div><span>JOINED</span><strong>{data.joinedParticipants}</strong></div>
      <div><span>IN PROGRESS</span><strong>{data.activeParticipants}</strong></div>
      <div><span>SUBMITTED</span><strong>{data.submittedParticipants}</strong></div>
      <div><span>WAITING</span><strong>{data.waitingParticipants}</strong></div>
      <div><span>STATUS</span><strong>{data.status}</strong></div>
    </div>

    <div className="monitor-grid">
      <div className="table-panel">
        <div className="table-head"><div><h2>Participant activity</h2><p>Current state of every joined participant.</p></div><span className="live-indicator">● LIVE</span></div>
        {!data.participants?.length ? <div className="table-empty"><h3>No participants have joined yet</h3></div> :
          <div className="host-list">{data.participants.map(p => <div className="host-row" key={p.participantId}>
            <div className="host-info"><strong>{p.name}</strong><span>{p.email || "No email"} · {p.answered}/{p.totalQuestions} answered</span></div>
            <div className="host-row-actions"><span className={`status-pill ${statusClass(p.status)}`}>{p.status.replace("_", " ")}</span><span className="monitor-time">{p.submittedAt ? `Submitted ${timeSince(p.submittedAt)}` : p.startedAt ? `Started ${timeSince(p.startedAt)}` : "Not started"}</span></div>
          </div>)}</div>}
      </div>

      <div className="table-panel monitor-activity-panel">
        <div className="table-head"><div><h2>Activity signals</h2><p>Aggregated browser events recorded during attempts.</p></div></div>
        {Object.keys(events).length === 0 ? <div className="table-empty"><h3>No activity events recorded</h3><p>Signals appear as participants interact with the competition.</p></div> : <div className="activity-list">{Object.entries(events).map(([key,value]) => <div className="activity-item" key={key}><span>{key.replaceAll("_", " ")}</span><strong>{value}</strong></div>)}</div>}
      </div>
    </div>
  </div>;
}
