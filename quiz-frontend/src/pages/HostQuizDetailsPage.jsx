import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../api/apiClient";

export default function HostQuizDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [quiz, setQuiz] = useState(null);
  const [error, setError] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [controlBusy, setControlBusy] = useState(false);
  const [publicationBusy, setPublicationBusy] = useState(false);

  useEffect(() => {
    let active = true;
    const load = () => api.getHostQuiz(id).then(d => { if (active) setQuiz(d); }).catch(e => { if (active) setError(e.message || "Unable to load quiz."); });
    load();
    const timer = window.setInterval(load, 5000);
    return () => { active = false; window.clearInterval(timer); };
  }, [id]);

  async function startCompetition() {
    setControlBusy(true); setError("");
    try { const next = await api.startQuiz(id); setQuiz(next); }
    catch (e) { setError(e.message || "Unable to start competition."); }
    finally { setControlBusy(false); }
  }

  async function toggleCompetition() {
    setControlBusy(true); setError("");
    try {
      const next = quiz.status === "CLOSED" ? await api.reopenQuiz(id) : await api.closeQuiz(id);
      setQuiz(next);
    } catch (e) { setError(e.message || "Unable to update competition status."); }
    finally { setControlBusy(false); }
  }

  async function updatePublication(patch) {
    setPublicationBusy(true); setError("");
    try { const next = await api.updateQuizPublication(id, patch); setQuiz(next); }
    catch (e) { setError(e.message || "Unable to update result/certificate settings."); }
    finally { setPublicationBusy(false); }
  }

  async function remove() {
    if (!window.confirm("Delete this quiz permanently? This action cannot be undone.")) return;
    setDeleting(true); setError("");
    try { await api.deleteQuiz(id); navigate("/host"); }
    catch (e) { setError(e.message || "Unable to delete quiz."); setDeleting(false); }
  }

  if (error) return <div className="alert error">{error}</div>;
  if (!quiz) return <div className="empty-panel"><h2>Loading quiz…</h2></div>;

  return <div className="dashboard-form-page">
    <div className="form-page-top"><div><span className="eyebrow">QUIZ DETAILS</span><h1>{quiz.title}</h1><p>{quiz.description || "No description provided."}</p></div><Link className="button-ghost dark" to="/host">← Back</Link></div>
    <div className="details-panel" style={{ marginBottom: 18 }}><div><span>STATUS</span><strong>{quiz.status}</strong></div><div><span>DURATION</span><strong>{quiz.durationMinutes || 10} min</strong></div><div><span>QUESTIONS</span><strong>{quiz.questions?.length || 0}</strong></div><div><span>CREATED</span><strong>{quiz.createdAt ? new Date(quiz.createdAt).toLocaleString() : "—"}</strong></div><div><span>UPDATED</span><strong>{quiz.updatedAt ? new Date(quiz.updatedAt).toLocaleString() : "—"}</strong></div></div>
    <div className="schedule-info"><div><span>SCHEDULE</span><strong>{quiz.scheduledStartAt ? new Date(quiz.scheduledStartAt).toLocaleString() : "Available after publish"}</strong></div><div><span>ENDS</span><strong>{quiz.scheduledEndAt ? new Date(quiz.scheduledEndAt).toLocaleString() : "No fixed end"}</strong></div>{quiz.status === "PUBLISHED" && <button className="button-primary small" disabled={controlBusy} onClick={startCompetition}>{controlBusy ? "Starting…" : "Start competition →"}</button>}{quiz.status !== "DRAFT" && <button className={quiz.status === "CLOSED" ? "button-primary small" : "button-danger small"} disabled={controlBusy} onClick={toggleCompetition}>{controlBusy ? "Updating…" : quiz.status === "CLOSED" ? "Reopen competition" : "Close competition"}</button>}</div>
    <div className="form-card publication-card"><div className="section-title-row"><div><h2>Results & certificate publishing</h2><p>Control what participants can use after submitting and whether ranked certificates are available.</p></div></div>
      <div className="publication-grid">
        <label className="toggle-card"><span><strong>Publish results</strong><small>Participants can view detailed results and rankings.</small></span><input type="checkbox" checked={quiz.resultPublished !== false} disabled={publicationBusy} onChange={e=>updatePublication({resultPublished:e.target.checked})}/></label>
        <label className="toggle-card"><span><strong>Enable certificates</strong><small>Offer printable achievement certificates to the top ranks.</small></span><input type="checkbox" checked={quiz.certificateEnabled === true} disabled={publicationBusy} onChange={e=>updatePublication({certificateEnabled:e.target.checked})}/></label>
      </div>
      {quiz.certificateEnabled && <div className="publication-settings"><label><span>Certificate top ranks</span><input type="number" min="1" max="100" defaultValue={quiz.certificateTopRanks || 3} onBlur={e=>updatePublication({certificateTopRanks:Number(e.target.value)||3})}/></label><label><span>Prize / recognition text</span><input type="text" defaultValue={quiz.prizeDescription || ""} placeholder="e.g. Winner receives a trophy" onBlur={e=>updatePublication({prizeDescription:e.target.value})}/></label></div>}
    </div>

    <div className="form-card"><div className="section-title-row"><div><h2>Question details</h2><p>Review the complete question set, including correct answers.</p></div><div className="form-actions"><Link className="button-ghost dark small" to={`/host/quizzes/${id}/results`}>Results</Link><Link className="button-ghost dark small" to={`/host/quizzes/${id}/analytics`}>Analytics</Link><Link className="button-ghost dark small" to={`/host/quizzes/${id}/edit`}>Edit quiz</Link><button className="button-danger small" disabled={deleting} onClick={remove}>{deleting ? "Deleting…" : "Delete quiz"}</button></div></div>
      <div>{quiz.questions?.map((q, i) => <div className="question-review" key={i}><div className="question-head"><strong>Question {i + 1}</strong><span className="status-pill">{q.type}</span></div><h3>{q.question}</h3>{(q.options?.length > 0 || q.type === "TRUE_FALSE") && <div className="review-options">{(q.options?.length ? q.options : ["True", "False"]).map((o, j) => <div className={o === q.correctAnswer ? "review-option correct" : "review-option"} key={j}>{String.fromCharCode(65 + j)}. {o}{o === q.correctAnswer && <b>✓ Correct</b>}</div>)}</div>}<div className="correct-answer"><span>CORRECT ANSWER</span><strong>{q.correctAnswer}</strong></div></div>)}</div>
    </div>
  </div>;
}
