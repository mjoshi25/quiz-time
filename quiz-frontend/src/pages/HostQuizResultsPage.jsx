import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../api/apiClient";

function formatDuration(seconds) {
  if (seconds == null) return "—";
  const s = Math.max(0, Number(seconds));
  const m = Math.floor(s / 60);
  const sec = s % 60;
  return `${m}m ${String(sec).padStart(2, "0")}s`;
}

export default function HostQuizResultsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [selected, setSelected] = useState(null);
  const [tab, setTab] = useState("results");
  const [questionAnalytics, setQuestionAnalytics] = useState(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [detailLoading, setDetailLoading] = useState(false);

  async function load() {
    setError("");
    try {
      const [r, p] = await Promise.all([api.hostQuizResults(id), api.hostQuizParticipants(id)]);
      setData(r);
      setParticipants(p?.participants || []);
    } catch (e) {
      setError(e.message || "Unable to load competition results.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [id]);

  async function loadQuestionAnalytics() {
    setAnalyticsLoading(true);
    try { setQuestionAnalytics(await api.hostQuestionAnalytics(id)); }
    catch (e) { setError(e.message || "Unable to load question analytics."); }
    finally { setAnalyticsLoading(false); }
  }

  async function downloadResults() {
    try {
      const blob = await api.exportHostResults(id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a"); a.href = url; a.download = `quizora-results-${id}.csv`; a.click();
      URL.revokeObjectURL(url);
    } catch (e) { setError(e.message || "Unable to export results."); }
  }

  async function openAttempt(attemptId) {
    setDetailLoading(true);
    setError("");
    try {
      const d = await api.hostAttemptDetails(id, attemptId);
      setSelected(d);
    } catch (e) {
      setError(e.message || "Unable to load attempt details.");
    } finally {
      setDetailLoading(false);
    }
  }

  if (loading) return <div className="empty-panel"><h2>Loading competition results…</h2></div>;
  if (error && !data) return <div className="alert error">{error}</div>;
  if (!data) return null;

  const average = data.submittedAttempts ? Math.round((data.results.reduce((sum, r) => sum + Number(r.percentage || 0), 0) / data.submittedAttempts) * 10) / 10 : 0;
  const highest = data.results.length ? data.results[0].score : 0;

  return <div className="dashboard-form-page">
    <div className="form-page-top">
      <div><span className="eyebrow">COMPETITION RESULTS</span><h1>{data.title}</h1><p>Monitor participants, submitted attempts and detailed scoring.</p></div>
      <div className="form-actions"><Link className="button-ghost dark" to={`/host/quizzes/${id}`}>← Quiz details</Link><button className="button-ghost dark" onClick={() => navigate("/host")}>Host centre</button><Link className="button-primary small" to={`/host/quizzes/${id}/monitor`}>Live monitor</Link><Link className="button-primary small" to={`/host/quizzes/${id}/analytics`}>Analytics</Link></div>
    </div>

    {error && <div className="alert error">{error}</div>}

    <div className="details-panel results-stats">
      <div><span>JOINED</span><strong>{data.joinedParticipants}</strong></div>
      <div><span>SUBMITTED</span><strong>{data.submittedAttempts}</strong></div>
      <div><span>WAITING / INCOMPLETE</span><strong>{data.notAttemptedOrIncomplete}</strong></div>
      <div><span>AVERAGE</span><strong>{average}%</strong></div>
      <div><span>HIGHEST</span><strong>{highest} / {data.totalQuestions}</strong></div>
    </div>

    <div className="results-tabs">
      <button className={tab === "results" ? "active" : ""} onClick={() => setTab("results")}>Leaderboard & results</button>
      <button className={tab === "participants" ? "active" : ""} onClick={() => setTab("participants")}>Participants ({participants.length})</button>
      <button className={tab === "analytics" ? "active" : ""} onClick={() => { setTab("analytics"); if (!questionAnalytics) loadQuestionAnalytics(); }}>Question analytics</button>
    </div>

    {tab === "results" ? <div className="table-panel">
      <div className="table-head"><div><h2>Submitted attempts</h2><p>Ranked by score, with earlier submission breaking ties.</p></div><button className="button-primary small" onClick={downloadResults}>Download CSV</button></div>
      {!data.results.length ? <div className="table-empty"><div className="empty-icon">◎</div><h3>No submitted attempts yet</h3><p>Participants who complete the quiz will appear here automatically.</p></div> :
        <div className="host-list">{data.results.map(row => <div className="host-row result-row" key={row.attemptId}>
          <div className="host-info"><strong>#{row.rank} {row.name}</strong><span>{row.email || "No email"} · {row.score} / {row.maxScore ?? row.totalQuestions} · {row.percentage}%</span></div>
          <div className="host-row-actions"><span className="status-pill approved">{formatDuration(row.timeTakenSeconds)}</span><button className="button-ghost dark small" onClick={() => openAttempt(row.attemptId)}>View attempt</button></div>
        </div>)}</div>}
    </div> : tab === "analytics" ? <div className="table-panel">
      <div className="table-head"><div><h2>Question analytics</h2><p>Aggregate performance across submitted attempts. Timing is browser-observed participant analytics.</p></div></div>
      {analyticsLoading ? <div className="table-empty"><h3>Loading question analytics…</h3></div> : !questionAnalytics?.questions?.length ? <div className="table-empty"><h3>No question analytics yet</h3><p>Analytics will appear after participants submit attempts.</p></div> : <div className="host-list">{questionAnalytics.questions.map(q => <div className="host-row" key={q.index}>
        <div className="host-info"><strong>Q{q.index + 1}. {q.question}</strong><span>{q.type} · {q.difficulty} · {q.correct} correct / {q.answered} answered · {q.unanswered} unanswered</span></div>
        <div className="host-row-actions"><span className="status-pill approved">{q.accuracy}% accuracy</span><span className="status-pill pending">{q.averageTimeSeconds}s avg</span></div>
      </div>)}</div>}
    </div> : <div className="table-panel">
      <div className="table-head"><div><h2>Joined participants</h2><p>Everyone who joined this competition, including people who have not submitted yet.</p></div><input className="host-search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search participant or email" /></div>
      {!participants.length ? <div className="table-empty"><h3>No participants have joined yet</h3></div> :
        <div className="host-list">{participants.filter(p => `${p.name || ""} ${p.email || ""}`.toLowerCase().includes(search.toLowerCase())).map(p => <div className="host-row" key={p.participantId}>
          <div className="host-info"><strong>{p.name}</strong><span>{p.email || "No email"} · Joined {p.joinedAt ? new Date(p.joinedAt).toLocaleString() : "—"}</span></div>
          <div className="host-row-actions">{p.attemptStatus === "SUBMITTED" ? <><span className="status-pill approved">{p.score} / {p.totalQuestions}</span>{data.results.find(r => r.participantId === p.participantId) && <button className="button-ghost dark small" onClick={() => openAttempt(data.results.find(r => r.participantId === p.participantId).attemptId)}>View result</button>}</> : <span className="status-pill pending">{p.attemptStatus === "NOT_SUBMITTED" ? "NOT SUBMITTED" : p.attemptStatus}</span>}</div>
        </div>)}</div>}
    </div>}

    {detailLoading && <div className="modal-backdrop"><div className="modal-card"><h2>Loading attempt…</h2></div></div>}
    {selected && <div className="modal-backdrop" onClick={() => setSelected(null)}><div className="modal-card attempt-detail-modal" onClick={e => e.stopPropagation()}>
      <div className="modal-head"><div><span className="eyebrow">ATTEMPT REVIEW</span><h2>{selected.participantName}</h2><p>{selected.participantEmail}</p></div><button className="modal-close" onClick={() => setSelected(null)}>×</button></div>
      <div className="attempt-summary"><div><span>SCORE</span><strong>{selected.score} / {selected.maxScore ?? selected.totalQuestions}</strong></div><div><span>CORRECT</span><strong>{selected.correctAnswers}</strong></div><div><span>TIME</span><strong>{formatDuration(selected.timeTakenSeconds)}</strong></div></div>
      <div className="attempt-questions">{selected.questions.map((q, i) => <div className={`attempt-question ${q.correct ? "is-correct" : "is-wrong"}`} key={i}><div className="question-head"><strong>Question {i + 1}</strong><span>{q.correct ? "✓ Correct" : "✕ Incorrect"}</span></div><h3>{q.question}</h3>{q.options?.length > 0 && <div className="review-options">{q.options.map((o, j) => <div className={o === q.correctAnswer ? "review-option correct" : "review-option"} key={j}>{String.fromCharCode(65 + j)}. {o}</div>)}</div>}<div className="answer-review"><div><span>PARTICIPANT ANSWER</span><strong>{q.givenAnswer || "Not answered"}</strong></div><div><span>CORRECT ANSWER</span><strong>{q.correctAnswer}</strong></div></div></div>)}</div>
    </div></div>}
  </div>;
}
