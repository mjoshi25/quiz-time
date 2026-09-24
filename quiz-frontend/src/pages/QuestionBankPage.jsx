import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/apiClient";

const blank = { type: "MCQ", question: "", options: ["", "", "", ""], correctAnswer: "", difficulty: "MEDIUM", explanation: "", imageUrl: "" };

function normalize(q) {
  return { ...blank, ...q, options: q.type === "TRUE_FALSE" ? ["True", "False"] : [...(q.options || []), "", "", "", ""].slice(0, 4) };
}

export default function QuestionBankPage() {
  const [items, setItems] = useState([]);
  const [search, setSearch] = useState("");
  const [form, setForm] = useState(blank);
  const [showForm, setShowForm] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const fileRef = useRef(null);

  async function load() { try { setItems(await api.questionBank() || []); } catch (e) { setError(e.message); } }
  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => items.filter(i => `${i.question} ${i.type} ${i.difficulty}`.toLowerCase().includes(search.toLowerCase())), [items, search]);

  function update(field, value) { setForm(p => ({ ...p, [field]: value })); }
  function updateOption(i, value) { setForm(p => ({ ...p, options: p.options.map((x,j) => j === i ? value : x) })); }

  async function save(e) {
    e.preventDefault(); setError(""); setMessage("");
    if (!form.question.trim() || !form.correctAnswer.trim()) return setError("Question text and correct answer are required.");
    try { await api.saveQuestionBankItem({ ...form, options: form.type === "TRUE_FALSE" ? ["True","False"] : form.options.filter(Boolean) }); setMessage("Question saved to your question bank."); setForm({ ...blank, options: ["","","",""] }); setShowForm(false); await load(); }
    catch (e) { setError(e.message || "Unable to save question."); }
  }

  async function remove(id) {
    if (!window.confirm("Delete this question from your question bank?")) return;
    try { await api.deleteQuestionBankItem(id); await load(); } catch (e) { setError(e.message); }
  }

  async function importFile(file) {
    if (!file) return; setError(""); setMessage("");
    try { const d = await api.importQuestionBank(file); setMessage(d?.message || "Questions imported."); await load(); }
    catch (e) { setError(e.message || "Unable to import CSV."); }
    finally { if (fileRef.current) fileRef.current.value = ""; }
  }

  async function exportCsv() {
    try { const blob = await api.exportQuestionBank(); const url = URL.createObjectURL(blob); const a = document.createElement("a"); a.href = url; a.download = "quizora-question-bank.csv"; a.click(); URL.revokeObjectURL(url); }
    catch (e) { setError(e.message || "Unable to export CSV."); }
  }

  return <div className="dashboard-form-page">
    <div className="form-page-top"><div><span className="eyebrow">HOST CENTRE</span><h1>Question bank</h1><p>Save reusable questions once and add them to future competitions.</p></div><div className="form-actions-inline"><Link className="button-ghost dark small" to="/host">← Host centre</Link><button className="button-ghost dark small" onClick={exportCsv}>Export CSV</button><a className="button-ghost dark small" href="/QUESTION_BANK_TEMPLATE.csv" download="QUESTION_BANK_TEMPLATE.csv">Template CSV</a><button className="button-ghost dark small" onClick={() => fileRef.current?.click()}>Import CSV</button><input ref={fileRef} hidden type="file" accept=".csv,text/csv" onChange={e => importFile(e.target.files?.[0])} /><button className="button-primary small" onClick={() => setShowForm(v => !v)}>+ Add question</button></div></div>
    {message && <div className="alert success">{message}</div>}{error && <div className="alert error">{error}</div>}
    {showForm && <form className="form-card bank-add-card" onSubmit={save}><div className="section-title-row"><div><h2>Add to question bank</h2><p>These questions are private to your host account.</p></div></div><div className="schedule-grid"><label>Question type<select value={form.type} onChange={e => update("type", e.target.value)}><option value="MCQ">Multiple Choice</option><option value="TRUE_FALSE">True / False</option><option value="CORRECT_WORD">Correct Word Choice</option></select></label><label>Difficulty<select value={form.difficulty} onChange={e => update("difficulty", e.target.value)}><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></select></label></div><label>Question<input value={form.question} onChange={e => update("question", e.target.value)} required /></label>{form.type === "TRUE_FALSE" ? <label>Correct answer<select value={form.correctAnswer} onChange={e => update("correctAnswer", e.target.value)}><option value="">Select</option><option>True</option><option>False</option></select></label> : <><div className="option-grid">{form.options.map((o,i)=><label key={i}>Option {String.fromCharCode(65+i)}<input value={o} onChange={e => updateOption(i,e.target.value)} /></label>)}</div><label>Correct answer<select value={form.correctAnswer} onChange={e => update("correctAnswer", e.target.value)}><option value="">Select</option>{form.options.filter(Boolean).map((o,i)=><option key={i}>{o}</option>)}</select></label></>}<label>Explanation<textarea rows="3" value={form.explanation} onChange={e => update("explanation", e.target.value)} /></label><label>Image URL (optional)<input value={form.imageUrl} onChange={e => update("imageUrl", e.target.value)} /></label><div className="form-actions"><button className="button-ghost dark" type="button" onClick={() => setShowForm(false)}>Cancel</button><button className="button-primary" type="submit">Save to bank</button></div></form>}
    <div className="table-panel"><div className="table-head"><div><h2>{items.length} saved questions</h2><p>Open a quiz editor and use “Question Bank” to reuse these questions.</p></div><input className="table-search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search questions..." /></div>{!filtered.length ? <div className="table-empty"><div className="empty-icon">◇</div><h3>No matching questions</h3><p>Add a question or import the CSV template to build your reusable library.</p></div> : <div className="bank-list">{filtered.map(i => <div className="bank-row" key={i.id}><div className="bank-info"><div className="bank-meta"><span className="status-pill approved">{i.difficulty}</span><span>{i.type}</span></div><strong>{i.question}</strong><small>{(i.options || []).filter(Boolean).join(" · ") || "True / False"}</small></div><button className="button-danger small" onClick={() => remove(i.id)}>Delete</button></div>)}</div>}</div>
    <div className="form-card"><h2>CSV format</h2><p>Use exactly these columns in row 1:</p><code>type,question,optionA,optionB,optionC,optionD,correctAnswer,difficulty,explanation,imageUrl</code><p className="muted-text">For MCQ use at least two options. For TRUE_FALSE use True or False as the answer. The importer supports quoted commas and quotes inside CSV values.</p></div>
  </div>;
}
