import { useEffect, useState } from "react";
import { api } from "../api/apiClient";

const fmt = n => Number(n || 0).toLocaleString();
const pct = (a,b) => b ? `${Math.round(a / b * 100)}%` : "0%";
function downloadBlob(blob, name){ const url=URL.createObjectURL(blob); const a=document.createElement("a"); a.href=url; a.download=name; a.click(); URL.revokeObjectURL(url); }

export default function AdminAnalyticsPage(){
  const [data,setData]=useState(null); const [range,setRange]=useState(null); const [from,setFrom]=useState(""); const [to,setTo]=useState(""); const [error,setError]=useState(""); const [loading,setLoading]=useState(true);
  async function load(){ try{setLoading(true);setError("");setData(await api.adminAnalytics());}catch(e){setError(e.message||"Unable to load analytics.")}finally{setLoading(false)} }
  useEffect(()=>{load(); const t=setInterval(load,30000); return()=>clearInterval(t)},[]);
  async function exportReport(){try{downloadBlob(await api.exportAdminAnalytics(),"quizora-platform-analytics.csv")}catch(e){setError(e.message||"Export failed.")}}
  async function loadRange(){try{if(!from||!to){setError("Select both dates.");return}setError("");setRange(await api.adminAnalyticsRange(from,to))}catch(e){setError(e.message||"Unable to load date-range analytics.")}}
  if(loading&&!data) return <main className="page-shell"><section className="hero-card"><span className="eyebrow">ADMIN INTELLIGENCE</span><h1>Platform analytics</h1><p>Loading competition statistics…</p></section></main>;
  if(error&&!data) return <main className="page-shell"><div className="alert error">{error}</div></main>;
  const t=data?.totals||{}; const trend=data?.trend||[]; const max=Math.max(1,...trend.map(x=>Math.max(x.joins,x.submissions,x.users,x.quizzes)));
  return <main className="page-shell">
    <section className="hero-card"><div className="section-title-row"><div><span className="eyebrow">ADMIN INTELLIGENCE</span><h1>Platform analytics</h1><p>Operational statistics across users, hosts, competitions and participation.</p></div><div className="form-actions-inline"><button className="button-ghost dark" onClick={load}>↻ Refresh</button><button className="button-primary" onClick={exportReport}>Download CSV</button></div></div></section>
    {error&&<div className="alert error">{error}</div>}
    <section className="stat-grid admin-stat-grid">
      {[['Users',t.users],['Participants',t.participants],['Hosts',t.hosts],['Approved hosts',t.approvedHosts],['Pending hosts',t.pendingHosts],['Competitions',t.quizzes],['Live competitions',t.startedQuizzes],['Submissions',t.submittedAttempts]].map(([label,value])=><div className="stat-card" key={label}><span>{label}</span><strong>{fmt(value)}</strong></div>)}
    </section>
    <div className="analytics-two-col">
      <section className="table-panel"><div className="table-head"><div><h2>30-day activity</h2><p>Daily users, quizzes, joins and submitted attempts (UTC).</p></div></div><div className="activity-chart">{trend.map(x=><div className="activity-day" key={x.date} title={`${x.date}: ${x.joins} joins · ${x.submissions} submissions`}><div className="activity-bars"><i style={{height:`${Math.max(4,x.joins/max*100)}%`}}/><i style={{height:`${Math.max(4,x.submissions/max*100)}%`}}/></div><small>{x.date.slice(5)}</small></div>)}</div></section>
      <section className="table-panel"><div className="table-head"><div><h2>Platform mix</h2><p>Current account and competition distribution.</p></div></div><div className="metric-list"><div><span>Approved hosts</span><strong>{pct(t.approvedHosts,t.hosts)}</strong></div><div><span>Pending hosts</span><strong>{pct(t.pendingHosts,t.hosts)}</strong></div><div><span>Started competitions</span><strong>{pct(t.startedQuizzes,t.quizzes)}</strong></div><div><span>Closed competitions</span><strong>{pct(t.closedQuizzes,t.quizzes)}</strong></div><div><span>Suspended quizzes</span><strong>{fmt(t.suspendedQuizzes)}</strong></div><div><span>Suspended users</span><strong>{fmt(t.suspendedUsers)}</strong></div></div></section>
    </div>
    <section className="table-panel"><div className="table-head"><div><h2>Host performance summary</h2><p>Competition volume and participation for the latest host accounts.</p></div><span className="count-chip">{data?.hosts?.length||0} hosts</span></div><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Host</th><th>Approval</th><th>Quizzes</th><th>Joins</th><th>Submissions</th><th>Avg score</th></tr></thead><tbody>{(data?.hosts||[]).map(h=><tr key={h.id}><td><strong>{h.name||"—"}</strong><small>{h.email}</small></td><td>{h.approval}</td><td>{fmt(h.quizCount)}</td><td>{fmt(h.joins)}</td><td>{fmt(h.submissions)}</td><td>{h.averageScore}</td></tr>)}</tbody></table></div></section>
    <section className="table-panel"><div className="table-head"><div><h2>Competition activity</h2><p>Participation and submission counts for recent competitions.</p></div></div><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Competition</th><th>Status</th><th>Questions</th><th>Joins</th><th>Submissions</th><th>Avg score</th></tr></thead><tbody>{(data?.quizzes||[]).map(q=><tr key={q.id}><td><strong>{q.title}</strong><small>{q.createdAt?new Date(q.createdAt).toLocaleString():"—"}</small></td><td>{q.status}</td><td>{q.questions}</td><td>{q.joins}</td><td>{q.submissions}</td><td>{q.averageScore}</td></tr>)}</tbody></table></div></section>
  </main>
}
