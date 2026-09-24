import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import { api } from "../api/apiClient";
import { useAuth } from "../context/AuthContext";

export default function PublicQuizDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [quiz, setQuiz] = useState(null);
  const [joined, setJoined] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    const load = () => api.publicQuiz(id).then(d => { if (active) setQuiz(d); }).catch(e => { if (active) setError(e.message || "Quiz not found."); });
    load();
    const timer = window.setInterval(load, 5000);
    return () => { active = false; window.clearInterval(timer); };
  }, [id]);
  useEffect(() => { if (user?.role === "PARTICIPANT") api.joinStatus(id).then(d => setJoined(!!d?.joined)).catch(() => {}); }, [id, user?.role]);

  async function join() {
    if (!user) { navigate("/login", { state: { from: `/quizzes/${id}` } }); return; }
    if (user.role !== "PARTICIPANT") { setError("Only participant accounts can join quizzes."); return; }
    setBusy(true); setError("");
    try { const d = await api.joinQuiz(id); setJoined(true); setMessage(d?.message || "Quiz joined successfully."); }
    catch (e) { setError(e.message || "Unable to join quiz."); }
    finally { setBusy(false); }
  }

  return <div className="public-page"><Navbar/><main className="listing-page">{error && <div className="alert error">{error}</div>}{!quiz ? <div className="empty-panel"><h2>Loading quiz…</h2></div> : <><div className="page-heading public-heading"><div><span className="eyebrow">{quiz.status === "STARTED" ? "LIVE COMPETITION" : "PUBLISHED COMPETITION"}</span><h1>{quiz.title}</h1><p>{quiz.description || "Test your knowledge in this competition."}</p></div><Link className="button-ghost" to="/quizzes">← All quizzes</Link></div><div className="quiz-detail-hero"><div><strong>{quiz.questionCount}</strong><span>Questions</span></div><div><strong>{quiz.durationMinutes || 10}m</strong><span>Time limit</span></div><div className="quiz-detail-action">{quiz.status === "CLOSED" ? <Link className="button-primary" to={`/quizzes/${id}/results`}>View final results →</Link> : quiz.status !== "STARTED" ? <button className="button-ghost dark" disabled>Waiting for host to start</button> : joined ? <Link className="button-primary" to={`/quiz/${id}/start`}>Start quiz →</Link> : <button className="button-primary" onClick={join} disabled={busy}>{busy ? "Joining…" : "Join quiz →"}</button>}</div></div>{message && <div className="alert success">{message}</div>}<div className="form-card"><h2>Competition information</h2><p className="muted">This competition contains {quiz.questionCount} question{quiz.questionCount === 1 ? "" : "s"}. Each participant gets {quiz.durationMinutes || 10} minutes after starting. {quiz.status === "STARTED" ? "The host has started the competition and participants can join now." : quiz.status === "CLOSED" ? "The competition is closed. Published final results are available from the results page." : "The host has published the competition; participation will open when the host starts it."} {quiz.scheduledStartAt ? `Scheduled start: ${new Date(quiz.scheduledStartAt).toLocaleString()}.` : ""} {quiz.scheduledEndAt ? `Scheduled end: ${new Date(quiz.scheduledEndAt).toLocaleString()}.` : ""}</p><div className="quiz-tags public-detail-tags"><span>{quiz.category || "Other"}</span>{quiz.topic && <span>{quiz.topic}</span>}<span>{quiz.resultPublished ? "Results will be public after closing" : "Results are not publicly published"}</span></div><div className="guest-action-panel"><strong>{user ? "Ready to participate?" : "New to Quizora?"}</strong><p>{user ? "Join the live competition to begin when the host has started it." : "You can browse Quizora without an account. Sign in or create a participant account when you want to join."}</p>{!user && <div className="hero-actions"><Link className="button-primary small" to="/register" state={{ from: `/quizzes/${id}` }}>Create participant account</Link><Link className="button-ghost dark small" to="/login" state={{ from: `/quizzes/${id}` }}>Sign in to join</Link></div>}</div></div></>}</main></div>;
}
