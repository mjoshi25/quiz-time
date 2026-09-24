import Brand from "../components/Brand";import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../api/apiClient";

export default function QuizAttemptPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [attempt, setAttempt] = useState(null);
  const [answers, setAnswers] = useState({});
  const [remaining, setRemaining] = useState(0);
  const [current, setCurrent] = useState(0);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState(null);
  const [error, setError] = useState("");
  const answersRef = useRef({});
  const submitLock = useRef(false);
  const saveTimer = useRef(null);
  const questionTimesRef = useRef({});

  useEffect(() => { answersRef.current = answers; }, [answers]);

  useEffect(() => {
    let active = true;
    api.startAttempt(id).then(data => {
      if (!active) return;
      if (data?.status === "SUBMITTED") { navigate(`/quiz/${id}/result`, { replace: true }); return; }
      const localKey = `quizora_attempt_${id}`;
      let local = {};
      try { local = JSON.parse(localStorage.getItem(localKey) || "{}"); } catch {}
      const recovered = { ...(data.answers || {}), ...(local.answers || {}) };
      questionTimesRef.current = { ...(data.questionTimeSeconds || {}), ...(local.questionTimeSeconds || {}) };
      setAttempt(data); setAnswers(recovered); answersRef.current = recovered;
      setRemaining(data.remainingSeconds || data.durationSeconds || 600);
      if (Object.keys(local.answers || {}).length) setSavedAt(new Date(local.savedAt || Date.now()));
    }).catch(e => setError(e.message || "Unable to start quiz.")).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id, navigate]);

  useEffect(() => {
    if (!attempt || remaining <= 0) return;
    const timer = setInterval(() => setRemaining(v => Math.max(0, v - 1)), 1000);
    return () => clearInterval(timer);
  }, [attempt]);

  useEffect(() => {
    if (attempt && remaining === 0 && !submitLock.current) submit(true);
  }, [remaining, attempt]);

  // Approximate time spent per question. This is client-observed time and is intended for participant analytics, not anti-cheat enforcement.
  useEffect(() => {
    if (!attempt) return;
    const timer = setInterval(() => {
      questionTimesRef.current[current] = (questionTimesRef.current[current] || 0) + 1;
    }, 1000);
    return () => clearInterval(timer);
  }, [attempt, current]);

  // Save locally immediately, then debounce a server save.
  useEffect(() => {
    if (!attempt) return;
    const localKey = `quizora_attempt_${id}`;
    try { localStorage.setItem(localKey, JSON.stringify({ answers, questionTimeSeconds: questionTimesRef.current, savedAt: Date.now() })); } catch {}
    if (saveTimer.current) clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(async () => {
      if (submitLock.current) return;
      setSaving(true);
      try { await api.saveAttempt(id, answersRef.current, questionTimesRef.current); setSavedAt(new Date()); }
      catch (_) { /* local recovery remains available */ }
      finally { setSaving(false); }
    }, 650);
    return () => clearTimeout(saveTimer.current);
  }, [answers, id, attempt]);

  // Record useful competition activity. Counts are stored server-side on the attempt.
  useEffect(() => {
    if (!attempt) return;
    const onVisibility = () => { if (document.hidden) api.recordAttemptActivity(id, "TAB_SWITCH").catch(() => {}); };
    const onFullscreen = () => { if (!document.fullscreenElement) api.recordAttemptActivity(id, "FULLSCREEN_EXIT").catch(() => {}); };
    document.addEventListener("visibilitychange", onVisibility);
    document.addEventListener("fullscreenchange", onFullscreen);
    return () => { document.removeEventListener("visibilitychange", onVisibility); document.removeEventListener("fullscreenchange", onFullscreen); };
  }, [attempt, id]);

  const question = attempt?.questions?.[current];
  const progress = useMemo(() => attempt?.questions?.length ? Math.round(((current + 1) / attempt.questions.length) * 100) : 0, [attempt, current]);
  const answeredCount = attempt ? Object.keys(answers).filter(k => answers[k] !== "" && answers[k] != null).length : 0;

  function choose(value) { setAnswers(prev => ({ ...prev, [current]: value })); }
  function formatTime(seconds) { const m=Math.floor(seconds/60).toString().padStart(2,"0"); const s=(seconds%60).toString().padStart(2,"0"); return `${m}:${s}`; }

  async function toggleFullscreen() {
    try {
      if (!document.fullscreenElement) await document.documentElement.requestFullscreen();
      else await document.exitFullscreen();
    } catch (_) {}
  }

  async function submit(auto=false) {
    if (!attempt || submitLock.current) return;
    if (!auto && !window.confirm("Submit this quiz now? You will not be able to change your answers after submission.")) return;
    submitLock.current = true; setSubmitting(true); setError("");
    try {
      const result = await api.submitAttempt(id, answersRef.current, questionTimesRef.current);
      try { localStorage.removeItem(`quizora_attempt_${id}`); } catch {}
      navigate(`/quiz/${id}/result`, { replace: true, state: { autoSubmitted: auto, result } });
    } catch(e) { submitLock.current = false; setError(e.message || "Unable to submit quiz."); setSubmitting(false); }
  }

  if (loading) return <div className="public-page"><main className="listing-page"><div className="empty-panel"><h2>Preparing your quiz…</h2></div></main></div>;
  if (error && !attempt) return <div className="public-page"><main className="listing-page"><div className="alert error">{error}</div><Link className="button-primary" to="/quizzes">Back to quizzes</Link></main></div>;
  if (!attempt) return null;

  const options = question?.options?.length ? question.options : (question?.type === "TRUE_FALSE" ? ["True", "False"] : []);
  return <div className="quiz-play-page">
    <header className="quiz-play-header">
      <Brand to="/dashboard" />
      <div className="quiz-header-title"><strong>{attempt.title}</strong><small>{answeredCount}/{attempt.questions.length} answered</small></div>
      <div className={`timer ${remaining < 60 ? "danger" : ""}`}><span>TIME LEFT</span><strong>{formatTime(remaining)}</strong></div>
      <button className="button-ghost dark quiz-fullscreen" onClick={toggleFullscreen}>⛶ Fullscreen</button>
    </header>
    <main className="quiz-play-shell">
      <div className="quiz-progress"><div><span>Question {current + 1} of {attempt.questions.length}</span><strong>{progress}%</strong></div><div className="progress-track"><span style={{width:`${progress}%`}} /></div></div>
      <div className="competition-layout">
        <aside className="question-map">
          <div><strong>Questions</strong><span>{answeredCount}/{attempt.questions.length}</span></div>
          <div className="question-map-grid">{attempt.questions.map((_,i)=><button key={i} className={`${i===current?"active ":""}${answers[i]!=null&&answers[i]!==""?"answered":""}`} onClick={()=>setCurrent(i)}>{i+1}</button>)}</div>
          <div className="question-map-legend"><span><i className="legend-current"/>Current</span><span><i className="legend-answered"/>Answered</span></div>
        </aside>
        <section className="quiz-question-card">
          <div className="question-meta"><span className="status-pill">{question.type}</span>{question.difficulty && <span className="difficulty-pill">{question.difficulty}</span>}</div>
          <h1>{question.question}</h1>
          {question.imageUrl && <img className="competition-question-image" src={question.imageUrl} alt="Question" onError={e => { e.currentTarget.style.display="none"; }} />}
          {options.length ? <div className="answer-grid">{options.map((option,i)=><button key={i} className={answers[current]===option?"answer-option selected":"answer-option"} onClick={()=>choose(option)}><span>{String.fromCharCode(65+i)}</span><strong>{option}</strong></button>)}</div> : <input className="quiz-text-answer" placeholder="Type your answer" value={answers[current] || ""} onChange={e=>choose(e.target.value)} />}
          {error && <div className="alert error">{error}</div>}
        </section>
      </div>
      <div className="quiz-save-status">{saving ? "Saving answer…" : savedAt ? `Saved ${savedAt.toLocaleTimeString([], {hour:"2-digit", minute:"2-digit", second:"2-digit"})}` : "Answers are saved automatically"}</div>
      <div className="quiz-navigation"><button className="button-ghost dark" disabled={current===0} onClick={()=>setCurrent(v=>v-1)}>← Previous</button><div className="question-dots mobile-question-dots">{attempt.questions.map((_,i)=><button key={i} className={i===current?"active":answers[i]?"answered":""} onClick={()=>setCurrent(i)}>{i+1}</button>)}</div>{current===attempt.questions.length-1?<button className="button-primary" disabled={submitting} onClick={()=>submit(false)}>{submitting?"Submitting…":"Submit quiz ✓"}</button>:<button className="button-primary" onClick={()=>setCurrent(v=>v+1)}>Next →</button>}</div>
    </main>
  </div>;
}
