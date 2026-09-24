import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/apiClient";
function duration(s){if(s==null)return "—";const n=Number(s),m=Math.floor(n/60);return `${m}m ${String(n%60).padStart(2,"0")}s`;}
export default function HostQuizAnalyticsPage(){
 const {id}=useParams(); const [data,setData]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState("");
 useEffect(()=>{api.hostQuizAnalytics(id).then(setData).catch(e=>setError(e.message||"Unable to load analytics.")).finally(()=>setLoading(false));},[id]);
 const hardest=useMemo(()=>data?.questions?.length?data.questions.reduce((a,b)=>Number(a.accuracy)>Number(b.accuracy)?b:a):null,[data]);
 if(loading)return <div className="empty-panel"><h2>Loading analytics…</h2></div>;
 if(error)return <div className="alert error">{error}</div>;
 if(!data)return null;
 const dist=data.scoreDistribution||{};
 return <div className="dashboard-form-page"><div className="form-page-top"><div><span className="eyebrow">COMPETITION ANALYTICS</span><h1>{data.title}</h1><p>Understand participation, scoring, completion and question performance.</p></div><div className="form-actions"><Link className="button-ghost dark" to={`/host/quizzes/${id}`}>← Quiz details</Link><Link className="button-ghost dark" to={`/host/quizzes/${id}/results`}>Results</Link></div></div>
  <div className="stats-grid"><div className="stat-card"><span>COMPLETION RATE</span><strong>{data.completionRate}%</strong><small>{data.submittedAttempts} of {data.joinedParticipants} joined</small></div><div className="stat-card"><span>AVERAGE SCORE</span><strong>{data.averagePercentage}%</strong><small>{data.averageScore} points average</small></div><div className="stat-card"><span>AVG. TIME</span><strong>{duration(data.averageTimeSeconds)}</strong><small>Across submitted attempts</small></div><div className="stat-card"><span>HARDEST QUESTION</span><strong>{hardest?`Q${hardest.index+1}`:"—"}</strong><small>{hardest?`${hardest.accuracy}% accuracy`:"No data yet"}</small></div></div>
  <div className="analytics-grid"><div className="table-panel"><div className="table-head"><div><h2>Score distribution</h2><p>Submitted participants grouped by percentage.</p></div></div><div className="analytics-bars">{Object.entries(dist).map(([label,count])=>{const max=Math.max(1,...Object.values(dist));return <div className="analytics-bar-row" key={label}><span>{label}</span><div className="analytics-bar-track"><div className="analytics-bar-fill" style={{width:`${Number(count)/max*100}%`}} /></div><strong>{count}</strong></div>})}</div></div>
  <div className="table-panel"><div className="table-head"><div><h2>Question performance</h2><p>Accuracy is calculated from submitted answers.</p></div></div><div className="host-list">{data.questions.map(q=><div className="host-row" key={q.index}><div className="host-info"><strong>Q{q.index+1}. {q.question}</strong><span>{q.type} · {q.correct} correct of {q.answered} answered</span></div><div className="host-row-actions"><span className={`status-pill ${q.difficulty==="HARD"?"pending":"approved"}`}>{q.difficulty}</span><strong>{q.accuracy}%</strong></div></div>)}</div></div></div>
 </div>;
}
