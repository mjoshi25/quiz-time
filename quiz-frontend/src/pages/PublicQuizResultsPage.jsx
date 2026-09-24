import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import { api } from "../api/apiClient";

function timeLabel(seconds) {
  if (!Number.isFinite(seconds)) return "—";
  const m=Math.floor(seconds/60), s=seconds%60;
  return `${m}m ${String(s).padStart(2,"0")}s`;
}

export default function PublicQuizResultsPage() {
  const { id }=useParams();
  const [data,setData]=useState(null), [error,setError]=useState(""), [loading,setLoading]=useState(true);
  useEffect(()=>{let active=true; api.publicQuizResults(id).then(d=>{if(active)setData(d)}).catch(e=>{if(active)setError(e.message||"Results are not available.")}).finally(()=>{if(active)setLoading(false)}); return()=>{active=false}},[id]);
  return <div className="public-page"><Navbar/><main className="listing-page">
    <div className="page-heading public-heading"><div><span className="eyebrow">OFFICIAL RESULTS</span><h1>{data?.title||"Competition results"}</h1><p>{data?.description||"Published competition results and winner highlights."}</p></div><Link className="button-ghost" to={data?`/quizzes/${data.quizId}`:"/quizzes"}>← Competition</Link></div>
    {loading&&<div className="empty-panel"><h2>Loading results…</h2></div>}
    {error&&!loading&&<div className="alert error">{error}</div>}
    {data&&!loading&&<>
      <div className="quiz-detail-hero"><div><strong>{data.totalParticipants}</strong><span>Participants</span></div><div><strong>{data.maxScore}</strong><span>Maximum score</span></div><div><strong>{data.leaderboard.length}</strong><span>Published results</span></div></div>
      {data.winners?.length>0&&<div className="form-card"><div className="table-head"><div><span className="eyebrow">WINNER HIGHLIGHTS</span><h2>Top performers</h2><p className="muted">Final rankings after the competition closed.</p></div></div><div className="winner-grid">{data.winners.map((w,i)=><div className={`winner-card winner-${Math.min(w.rank,3)}`} key={`${w.rank}-${w.participantName}`}><span className="winner-medal">{w.rank===1?"🏆":w.rank===2?"🥈":"🥉"}</span><strong>#{w.rank} {w.participantName}</strong><span>{w.score} points · {w.percentage}%</span>{data.prizeDescription&&<small>{data.prizeDescription}</small>}</div>)}</div></div>}
      <div className="table-panel leaderboard-panel"><div className="table-head"><div><h2>Final leaderboard</h2><p>Official published standings.</p></div><span className="status-pill approved">CLOSED</span></div>{!data.leaderboard.length?<div className="table-empty"><h3>No submitted results</h3></div>:<div className="host-list">{data.leaderboard.map(row=><div className="host-row" key={`${row.rank}-${row.participantName}`}><div className="host-info"><strong>#{row.rank} {row.participantName}</strong><span>{row.score}/{row.maxScore??data.maxScore} points · {row.percentage}% · {row.correctAnswers}/{row.total} correct · {timeLabel(row.timeTakenSeconds)}</span></div><span className="status-pill">FINAL</span></div>)}</div>}</div>
    </>}
  </main></div>
}
