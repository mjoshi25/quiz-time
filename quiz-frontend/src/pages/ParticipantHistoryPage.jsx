import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/apiClient";

function duration(s){if(s==null)return "—"; const n=Math.max(0,Number(s)),m=Math.floor(n/60);return `${m}m ${String(n%60).padStart(2,"0")}s`;}
export default function ParticipantHistoryPage(){
 const [rows,setRows]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState("");
 useEffect(()=>{api.participantHistory().then(d=>setRows(d?.attempts||[])).catch(e=>setError(e.message||"Unable to load history.")).finally(()=>setLoading(false));},[]);
 const completed=useMemo(()=>rows.filter(r=>r.status==="SUBMITTED"),[rows]);
 const avg=completed.length?Math.round(completed.reduce((a,r)=>a+Number(r.percentage||0),0)/completed.length*10)/10:0;
 const points=completed.reduce((a,r)=>a+Number(r.score||0),0);
 return <div className="dashboard-form-page">
  <div className="form-page-top"><div><span className="eyebrow">MY PERFORMANCE</span><h1>Quiz history</h1><p>Review your completed competitions, scores and time taken.</p></div><Link className="button-ghost dark" to="/quizzes">Explore quizzes →</Link></div>
  <div className="stats-grid"><div className="stat-card"><span>COMPLETED</span><strong>{completed.length}</strong><small>Submitted attempts</small></div><div className="stat-card"><span>TOTAL POINTS</span><strong>{points}</strong><small>Across completed quizzes</small></div><div className="stat-card"><span>AVERAGE</span><strong>{avg}%</strong><small>Average percentage</small></div></div>
  {error&&<div className="alert error">{error}</div>}
  <div className="table-panel"><div className="table-head"><div><h2>Attempt history</h2><p>Your results are stored against your participant account.</p></div></div>
   {loading?<div className="table-empty"><h3>Loading history…</h3></div>:!rows.length?<div className="table-empty"><h3>No attempts yet</h3><p>Join a published competition and complete it to see your history.</p><Link className="button-primary" to="/quizzes">Find a competition</Link></div>:
   <div className="host-list">{rows.map(r=><div className="host-row" key={r.attemptId}><div className="host-info"><strong>{r.title}</strong><span>{r.status} · {r.score} / {r.maxScore ?? r.totalQuestions} · {r.percentage}% · {duration(r.timeTakenSeconds)}</span></div><div className="host-row-actions">{r.submittedAt&&<span className="status-pill approved">{new Date(r.submittedAt).toLocaleDateString()}</span>}{r.status==="SUBMITTED"&&<Link className="button-ghost dark small" to={`/quiz/${r.quizId}/result`}>View result</Link>}</div></div>)}</div>}
  </div>
 </div>;
}
