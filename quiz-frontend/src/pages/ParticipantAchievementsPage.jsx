import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/apiClient";

function duration(s){if(s==null)return "—";const n=Math.max(0,Number(s));return `${Math.floor(n/60)}m ${String(n%60).padStart(2,"0")}s`;}

export default function ParticipantAchievementsPage(){
 const [data,setData]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState("");
 useEffect(()=>{let active=true;api.participantProfile().then(v=>{if(active)setData(v)}).catch(e=>{if(active)setError(e.message||"Unable to load achievements.")}).finally(()=>{if(active)setLoading(false)});return()=>{active=false}},[]);
 if(loading)return <div className="dashboard-form-page"><div className="empty-panel"><h2>Loading your achievements…</h2></div></div>;
 if(error)return <div className="dashboard-form-page"><div className="alert error">{error}</div></div>;
 const s=data?.stats||{};
 return <div className="dashboard-form-page">
  <div className="form-page-top"><div><span className="eyebrow">PARTICIPANT ACHIEVEMENTS</span><h1>Your performance journey</h1><p>Track completed competitions, milestones, certificates and recent results.</p></div><Link className="button-ghost dark" to="/quizzes">Explore quizzes →</Link></div>
  <div className="stats-grid">
   <div className="stat-card"><span>COMPLETED</span><strong>{s.completed||0}</strong><small>Submitted competitions</small></div>
   <div className="stat-card"><span>TOTAL POINTS</span><strong>{s.totalPoints||0}</strong><small>Across submitted attempts</small></div>
   <div className="stat-card"><span>AVERAGE</span><strong>{s.averagePercentage||0}%</strong><small>Average percentage</small></div>
   <div className="stat-card"><span>BEST RANK</span><strong>{s.bestRank?`#${s.bestRank}`:"—"}</strong><small>{s.certificates||0} certificate{s.certificates===1?"":"s"} earned</small></div>
  </div>
  <section className="table-panel"><div className="table-head"><div><h2>Achievement badges</h2><p>Milestones are calculated from submitted competition results.</p></div></div><div className="achievement-grid">{(data?.achievements||[]).map(a=><article className={`achievement-card ${a.unlocked?"unlocked":"locked"}`} key={a.id}><div className="achievement-icon">{a.unlocked?"★":"◇"}</div><div><span className="eyebrow">{a.unlocked?"UNLOCKED":"LOCKED"}</span><h3>{a.name}</h3><p>{a.description}</p></div></article>)}</div></section>
  <section className="table-panel"><div className="table-head"><div><h2>Recent performance</h2><p>Your latest submitted competition results.</p></div><Link className="button-ghost dark small" to="/history">Full history</Link></div>{!data?.recent?.length?<div className="table-empty"><h3>No completed competitions yet</h3><Link className="button-primary" to="/quizzes">Find a competition</Link></div>:<div className="host-list">{data.recent.map(r=><div className="host-row" key={`${r.quizId}-${r.submittedAt}`}><div className="host-info"><strong>{r.title}</strong><span>{r.score}/{r.maxScore} · {r.percentage}% · Rank {r.rank?`#${r.rank}`:"—"} · {duration(r.timeTakenSeconds)}</span></div><div className="host-row-actions"><span className="status-pill approved">{new Date(r.submittedAt).toLocaleDateString()}</span><Link className="button-ghost dark small" to={`/quiz/${r.quizId}/result`}>View result</Link></div></div>)}</div>}</section>
 </div>;
}
