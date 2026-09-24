import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../api/apiClient";

const emptyQuestion = () => ({
  type: "MCQ",
  question: "",
  options: ["", "", "", ""],
  correctAnswer: "",
  difficulty: "MEDIUM",
  explanation: "",
  imageUrl: ""
});

function normalizeQuestion(q) {
  return {
    type: q?.type || "MCQ",
    question: q?.question || "",
    options: q?.type === "TRUE_FALSE" ? ["True", "False"] : (Array.isArray(q?.options) ? [...q.options, "", "", "", ""].slice(0, 4) : ["", "", "", ""]),
    correctAnswer: q?.correctAnswer || "",
    difficulty: q?.difficulty || "MEDIUM",
    explanation: q?.explanation || "",
    imageUrl: q?.imageUrl || ""
  };
}

export default function QuizEditorPage({ edit = false, admin = false }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [quiz, setQuiz] = useState({ title: "", description: "", category: "Other", topic: "", durationMinutes: 10, pointsPerCorrect: 1, negativeMarkingEnabled: false, penaltyPerWrong: 0, allowRetake: false, maxAttempts: 1, scheduledStartAt: "", scheduledEndAt: "", questions: [emptyQuestion()] });
  const [loading, setLoading] = useState(edit);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState("");
  const [busy, setBusy] = useState(false);
  const [publishAfterSave, setPublishAfterSave] = useState(false);
  const [bankOpen, setBankOpen] = useState(false);
  const [bankItems, setBankItems] = useState([]);
  const [bankLoading, setBankLoading] = useState(false);
  const [bankMessage, setBankMessage] = useState("");

  useEffect(() => {
    if (!edit) return;
    (admin ? api.getAdminQuiz(id) : api.getHostQuiz(id))
      .then(data => setQuiz({
        title: data.title || "",
        description: data.description || "",
        category: data.category || "Other",
        topic: data.topic || "",
        durationMinutes: data.durationMinutes || 10,
        pointsPerCorrect: data.pointsPerCorrect ?? 1,
        negativeMarkingEnabled: data.negativeMarkingEnabled === true,
        penaltyPerWrong: data.penaltyPerWrong ?? 0,
        allowRetake: data.allowRetake === true,
        maxAttempts: data.maxAttempts ?? 1,
        scheduledStartAt: data.scheduledStartAt ? data.scheduledStartAt.slice(0,16) : "",
        scheduledEndAt: data.scheduledEndAt ? data.scheduledEndAt.slice(0,16) : "",
        questions: data.questions?.length ? data.questions.map(normalizeQuestion) : [emptyQuestion()]
      }))
      .catch(e => setError(e.message || "Unable to load quiz."))
      .finally(() => setLoading(false));
  }, [edit, id, admin]);

  function updateQuiz(field, value) { setQuiz(p => ({ ...p, [field]: value })); }
  function updateQuestion(index, field, value) {
    setQuiz(p => ({ ...p, questions: p.questions.map((q, i) => i === index
      ? { ...q, [field]: value, ...(field === "type" && value === "TRUE_FALSE" ? { options: ["True", "False"], correctAnswer: "" } : {}) }
      : q) }));
  }
  function updateOption(index, option, value) { setQuiz(p => ({ ...p, questions: p.questions.map((q, i) => i === index ? { ...q, options: q.options.map((o, j) => j === option ? value : o) } : q) })); }
  function addQuestion() { setQuiz(p => ({ ...p, questions: [...p.questions, emptyQuestion()] })); }
  function removeQuestion(index) { setQuiz(p => ({ ...p, questions: p.questions.filter((_, i) => i !== index) })); }
  async function openBank() {
    setBankOpen(true); setBankLoading(true); setBankMessage("");
    try { setBankItems(await api.questionBank() || []); }
    catch (e) { setBankMessage(e.message || "Unable to load question bank."); }
    finally { setBankLoading(false); }
  }
  function addFromBank(item) {
    setQuiz(p => ({ ...p, questions: [...p.questions, normalizeQuestion(item)] }));
    setBankMessage("Question added to this quiz.");
  }
  async function saveToBank(q) {
    try { await api.saveQuestionBankItem({ ...q, options: q.type === "TRUE_FALSE" ? ["True", "False"] : q.options.filter(Boolean) }); setBankMessage("Question saved to your question bank."); }
    catch (e) { setBankMessage(e.message || "Unable to save question to bank."); }
  }

  async function submit(e, publish = false) {
    e.preventDefault(); setError(""); setSaved(""); setPublishAfterSave(publish);
    if (!quiz.title.trim()) return setError("Quiz title is required.");
    if (!quiz.questions.length) return setError("Add at least one question.");
    if (Number(quiz.durationMinutes) < 1 || Number(quiz.durationMinutes) > 180) return setError("Duration must be between 1 and 180 minutes.");
    if (Number(quiz.pointsPerCorrect) < 1 || Number(quiz.pointsPerCorrect) > 100) return setError("Points per correct answer must be between 1 and 100.");
    if (quiz.negativeMarkingEnabled && (Number(quiz.penaltyPerWrong) < 0 || Number(quiz.penaltyPerWrong) > 100)) return setError("Wrong-answer penalty must be between 0 and 100.");
    if (quiz.allowRetake && (Number(quiz.maxAttempts) < 2 || Number(quiz.maxAttempts) > 20)) return setError("Maximum attempts must be between 2 and 20 when retakes are enabled.");
    for (const q of quiz.questions) {
      if (!q.question.trim() || !q.correctAnswer.trim()) return setError("Every question needs question text and a correct answer.");
      if (q.type === "MCQ" && q.options.filter(Boolean).length < 2) return setError("Each multiple-choice question needs at least two options.");
      if (q.type === "CORRECT_WORD" && q.options.filter(Boolean).length < 1) return setError("Correct Word Choice needs at least one option.");
    }
    setBusy(true);
    try {
      const payload = {
        ...quiz,
        durationMinutes: Number(quiz.durationMinutes || 10),
        scheduledStartAt: quiz.scheduledStartAt ? new Date(quiz.scheduledStartAt).toISOString() : null,
        scheduledEndAt: quiz.scheduledEndAt ? new Date(quiz.scheduledEndAt).toISOString() : null,
        questions: quiz.questions.map(q => ({ ...q, options: q.type === "TRUE_FALSE" ? ["True", "False"] : q.options.filter(Boolean) }))
      };
      const data = edit ? (admin ? await api.updateAdminQuiz(id, payload) : await api.updateQuiz(id, payload)) : await api.createQuiz(payload);
      if (publish) await api.publishQuiz(data.id);
      setSaved(publish ? "Quiz saved and published successfully." : "Quiz saved successfully.");
      setTimeout(() => navigate(admin ? "/admin" : "/host"), 700);
    } catch (e) { setError(e.message || "Unable to save quiz."); }
    finally { setBusy(false); }
  }

  if (loading) return <div className="empty-panel"><h2>Loading quiz…</h2></div>;

  return <div className="dashboard-form-page">
    <div className="form-page-top">
      <div><span className="eyebrow">{admin ? "ADMIN CONSOLE" : "HOST CENTRE"}</span><h1>{edit ? "Edit quiz" : "Create quiz"}</h1><p>{edit ? "Update the competition and question details." : "Build a richer competition with difficulty, explanations and optional question images."}</p></div>
      <Link className="button-ghost dark" to={admin ? "/admin" : "/host"}>← Back</Link>
    </div>
    <form className="quiz-form" onSubmit={e => submit(e, false)}>
      <section className="form-card"><h2>Competition details</h2><label>Quiz title<input required value={quiz.title} onChange={e => updateQuiz("title", e.target.value)} placeholder="e.g. General Knowledge Challenge" /></label><label>Description<textarea value={quiz.description} onChange={e => updateQuiz("description", e.target.value)} placeholder="Tell participants what this quiz is about." rows="4" /></label><div className="schedule-grid"><label>Category<select value={quiz.category} onChange={e => updateQuiz("category", e.target.value)}><option>General Knowledge</option><option>Science</option><option>Technology</option><option>India & World</option><option>Sports</option><option>History</option><option>Geography</option><option>Mathematics</option><option>Language</option><option>Current Affairs</option><option>Other</option></select></label><label>Topic / tags<input value={quiz.topic} onChange={e => updateQuiz("topic", e.target.value)} placeholder="e.g. Space, Physics, World History" /><small>Use a short topic phrase to make this competition easier to discover.</small></label></div><div className="schedule-grid"><label>Duration (minutes)<input type="number" min="1" max="180" value={quiz.durationMinutes} onChange={e => updateQuiz("durationMinutes", Number(e.target.value))} /><small>Participants get this much time after they start.</small></label><label>Points per correct answer<input type="number" min="1" max="100" value={quiz.pointsPerCorrect} onChange={e => updateQuiz("pointsPerCorrect", Number(e.target.value))} /><small>Every correct answer earns this many points.</small></label><label>Scheduled start (optional)<input type="datetime-local" value={quiz.scheduledStartAt || ""} onChange={e => updateQuiz("scheduledStartAt", e.target.value)} /><small>Leave blank to allow starting immediately after publish.</small></label><label>Scheduled end (optional)<input type="datetime-local" value={quiz.scheduledEndAt || ""} onChange={e => updateQuiz("scheduledEndAt", e.target.value)} /><small>New attempts are blocked after this time.</small></label></div><div className="scoring-settings"><label className="checkbox-row"><input type="checkbox" checked={quiz.negativeMarkingEnabled} onChange={e => updateQuiz("negativeMarkingEnabled", e.target.checked)} /> Enable negative marking</label>{quiz.negativeMarkingEnabled && <label>Penalty for each wrong answer<input type="number" min="0" max="100" value={quiz.penaltyPerWrong} onChange={e => updateQuiz("penaltyPerWrong", Number(e.target.value))} /><small>Unanswered questions receive no penalty. Example: +2 correct and −1 wrong.</small></label>}<div className="scoring-preview"><strong>Scoring preview:</strong> {quiz.pointsPerCorrect} point{Number(quiz.pointsPerCorrect)===1?"":"s"} for each correct answer{quiz.negativeMarkingEnabled ? `, −${quiz.penaltyPerWrong} for each wrong answer.` : "."}</div><div className="scoring-preview retake-settings"><label className="checkbox-row"><input type="checkbox" checked={quiz.allowRetake} onChange={e => updateQuiz("allowRetake", e.target.checked)} /> Allow participants to retake</label>{quiz.allowRetake && <label>Maximum attempts<input type="number" min="2" max="20" value={quiz.maxAttempts} onChange={e => updateQuiz("maxAttempts", Number(e.target.value))} /><small>Includes the first attempt. Set 2 for one retake.</small></label>}</div></div></section>
      <section className="form-card"><div className="section-title-row"><div><h2>Questions</h2><p>Add question metadata to improve participant experience and post-quiz review.</p></div><div className="form-actions-inline">{!admin && <button type="button" className="button-ghost dark small" onClick={openBank}>◇ Question Bank</button>}<button type="button" className="button-ghost dark small" onClick={addQuestion}>+ Add question</button></div></div>
        {quiz.questions.map((q, i) => <div className="question-editor" key={i}>
          <div className="question-head"><strong>Question {i + 1}</strong><div className="form-actions-inline">{!admin && <button type="button" className="text-link" onClick={() => saveToBank(q)}>Save to bank</button>}{quiz.questions.length > 1 && <button type="button" className="text-danger" onClick={() => removeQuestion(i)}>Remove</button>}</div></div>
          <div className="schedule-grid">
            <label>Question type<select value={q.type} onChange={e => updateQuestion(i, "type", e.target.value)}><option value="MCQ">Multiple Choice</option><option value="TRUE_FALSE">True / False</option><option value="CORRECT_WORD">Correct Word Choice</option></select></label>
            <label>Difficulty<select value={q.difficulty} onChange={e => updateQuestion(i, "difficulty", e.target.value)}><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></select></label>
          </div>
          <label>Question<input required value={q.question} onChange={e => updateQuestion(i, "question", e.target.value)} placeholder="Enter the question" /></label>
          <label>Question image URL (optional)<input value={q.imageUrl} onChange={e => updateQuestion(i, "imageUrl", e.target.value)} placeholder="https://..." /><small>Use a publicly accessible image URL. Image upload can be added in a later media phase.</small></label>
          {q.imageUrl && <div className="quiz-image-preview"><img src={q.imageUrl} alt="Question preview" onError={e => { e.currentTarget.style.display = "none"; }} /><span>Question image preview</span></div>}
          {q.type === "TRUE_FALSE" ? <><div className="review-options"><div className="review-option">A. True</div><div className="review-option">B. False</div></div><label>Correct answer<select value={q.correctAnswer} onChange={e => updateQuestion(i, "correctAnswer", e.target.value)}><option value="">Select answer</option><option value="True">True</option><option value="False">False</option></select></label></> : <><div className="option-grid">{q.options.map((o, j) => <label key={j}>Option {String.fromCharCode(65 + j)}<input value={o} onChange={e => updateOption(i, j, e.target.value)} placeholder={`Option ${String.fromCharCode(65 + j)}`} /></label>)}</div><label>Correct answer<select value={q.correctAnswer} onChange={e => updateQuestion(i, "correctAnswer", e.target.value)}><option value="">Select correct option</option>{q.options.filter(Boolean).map((o, j) => <option key={o + j} value={o}>{o}</option>)}</select></label></>}
          <label>Answer explanation (optional)<textarea rows="3" value={q.explanation} onChange={e => updateQuestion(i, "explanation", e.target.value)} placeholder="Explain why the correct answer is right. Participants can review this after submitting." /></label>
        </div>)}
      </section>
      {bankOpen && <div className="modal-backdrop" onClick={() => setBankOpen(false)}><div className="bank-modal" onClick={e => e.stopPropagation()}><div className="section-title-row"><div><span className="eyebrow">QUESTION BANK</span><h2>Reuse saved questions</h2><p>Select any question to append it to this quiz.</p></div><button className="button-ghost dark small" type="button" onClick={() => setBankOpen(false)}>Close</button></div>{bankMessage && <div className="alert success">{bankMessage}</div>}{bankLoading ? <div className="table-empty">Loading question bank…</div> : !bankItems.length ? <div className="table-empty"><h3>Your question bank is empty</h3><p>Save questions from this editor or import a CSV from the Question Bank page.</p><Link className="button-ghost dark small" to="/host/question-bank">Open question bank</Link></div> : <div className="bank-modal-list">{bankItems.map(item => <div className="bank-row" key={item.id}><div className="bank-info"><div className="bank-meta"><span className="status-pill approved">{item.difficulty}</span><span>{item.type}</span></div><strong>{item.question}</strong><small>{(item.options || []).filter(Boolean).join(" · ") || "True / False"}</small></div><button className="button-primary small" type="button" onClick={() => addFromBank(item)}>Add</button></div>)}</div>}</div></div>}{error && <div className="alert error">{error}</div>}{saved && <div className="alert success">{saved}</div>}
      <div className="form-actions"><button className="button-ghost dark" type="button" disabled={busy} onClick={e => submit(e, false)}>{busy && !publishAfterSave ? "Saving…" : "Save changes"}</button>{!admin && <button className="button-primary" type="button" disabled={busy} onClick={e => submit(e, true)}>{busy && publishAfterSave ? "Publishing…" : "Save & publish"}<span>→</span></button>}</div>
    </form>
  </div>;
}
