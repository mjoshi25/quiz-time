import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { api } from "../api/apiClient";
import { useAuth } from "../context/AuthContext";

const categories = ["General Knowledge","Science","Technology","India & World","Sports","History","Geography","Mathematics","Language","Current Affairs","Other"];

export default function QuizListPage() {
  const [quizzes, setQuizzes] = useState([]);
  const [joined, setJoined] = useState(new Set());
  const [meta, setMeta] = useState({ page: 0, totalPages: 0, totalElements: 0 });
  const [filters, setFilters] = useState({ q: "", category: "", difficulty: "", status: "", sort: "newest", page: 0, size: 9 });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    let active = true;
    setLoading(true);
    const timer = window.setTimeout(() => {
      api.publicQuizzes(filters).then(d => {
        if (!active) return;
        setQuizzes(Array.isArray(d?.content) ? d.content : []);
        setMeta(d || { page: filters.page, totalPages: 0, totalElements: 0 });
      }).catch(e => { if (active) setError(e.message || "Unable to load quizzes."); })
        .finally(() => { if (active) setLoading(false); });
    }, 180);
    return () => { active = false; window.clearTimeout(timer); };
  }, [filters]);

  useEffect(() => {
    if (user?.role === "PARTICIPANT") api.joinedQuizzes().then(d => setJoined(new Set((d || []).map(x => x.id)))).catch(() => {});
  }, [user?.role]);

  function setFilter(field, value) { setFilters(p => ({ ...p, [field]: value, page: field === "page" ? value : 0 })); }
  async function join(id) {
    if (!user) { navigate("/login", { state: { from: `/quizzes/${id}` } }); return; }
    if (user.role !== "PARTICIPANT") { setError("Only participant accounts can join quizzes."); return; }
    setBusy(id); setError("");
    try { await api.joinQuiz(id); setJoined(prev => new Set([...prev, id])); }
    catch (e) { setError(e.message || "Unable to join quiz."); }
    finally { setBusy(""); }
  }
  function statusLabel(status) { return status === "STARTED" ? "LIVE" : status === "CLOSED" ? "COMPLETED" : "UPCOMING"; }

  return <div className="public-page"><Navbar/><main className="listing-page">
    <div className="page-heading public-heading"><div><span className="eyebrow">PUBLIC COMPETITIONS</span><h1>Explore quizzes</h1><p>Search competitions by topic, category, difficulty and status.</p></div>{user?.role === "PARTICIPANT" ? <Link className="button-primary small" to="/dashboard">My joined quizzes</Link> : <Link className="button-ghost" to="/">Back home</Link>}</div>
    <section className="form-card discovery-filters">
      <div className="discovery-filter-row"><label className="discovery-search">Search<input value={filters.q} onChange={e => setFilter("q", e.target.value)} placeholder="Search title, description or topic…" /></label><label>Category<select value={filters.category} onChange={e => setFilter("category", e.target.value)}><option value="">All categories</option>{categories.map(c => <option key={c}>{c}</option>)}</select></label><label>Difficulty<select value={filters.difficulty} onChange={e => setFilter("difficulty", e.target.value)}><option value="">Any difficulty</option><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></select></label><label>Status<select value={filters.status} onChange={e => setFilter("status", e.target.value)}><option value="">All statuses</option><option value="STARTED">Live</option><option value="PUBLISHED">Upcoming</option><option value="CLOSED">Completed</option></select></label><label>Sort<select value={filters.sort} onChange={e => setFilter("sort", e.target.value)}><option value="newest">Newest</option><option value="oldest">Oldest</option><option value="title">Title A–Z</option><option value="questions">Most questions</option></select></label></div>
      <div className="discovery-summary"><span>{meta.totalElements || 0} competition{meta.totalElements === 1 ? "" : "s"} found</span>{(filters.q || filters.category || filters.difficulty || filters.status) && <button className="text-link" type="button" onClick={() => setFilters(p => ({ ...p, q:"", category:"", difficulty:"", status:"", page:0 }))}>Clear filters</button>}</div>
    </section>
    {error && <div className="alert error">{error}</div>}
    {loading ? <div className="empty-panel"><h2>Loading competitions…</h2></div> : !quizzes.length ? <div className="empty-panel"><div className="empty-icon">✦</div><h2>No matching competitions</h2><p>Try changing your search or filters.</p></div> : <div className="quiz-grid">{quizzes.map(q => <article className="quiz-card" key={q.id}><span className={`status-pill ${q.status === "STARTED" ? "approved" : q.status === "CLOSED" ? "" : "pending"}`}>{statusLabel(q.status)}</span><h2>{q.title}</h2><p>{q.description || "Test your knowledge in this competition."}</p><div className="quiz-tags"><span>{q.category || "Other"}</span>{q.topic && <span>{q.topic}</span>}</div><div className="quiz-meta"><span>{q.questionCount || 0} questions</span><span>{q.durationMinutes || 10} min</span></div><div className="quiz-card-actions"><Link className="button-ghost dark small" to={`/quizzes/${q.id}`}>View details</Link>{q.status === "CLOSED" ? <Link className="button-primary small" to={`/quizzes/${q.id}/results`}>View results</Link> : q.status !== "STARTED" ? <button className="button-ghost dark small" disabled>Waiting for host</button> : joined.has(q.id) ? <button className="button-primary small" disabled>✓ Joined</button> : <button className="button-primary small" disabled={busy === q.id} onClick={() => join(q.id)}>{busy === q.id ? "Joining…" : "Join quiz →"}</button>}</div></article>)}</div>}
    {meta.totalPages > 1 && <div className="pagination"><button className="button-ghost dark small" disabled={filters.page <= 0} onClick={() => setFilter("page", filters.page - 1)}>← Previous</button><span>Page {filters.page + 1} of {meta.totalPages}</span><button className="button-ghost dark small" disabled={filters.page + 1 >= meta.totalPages} onClick={() => setFilter("page", filters.page + 1)}>Next →</button></div>}
  </main></div>;
}
