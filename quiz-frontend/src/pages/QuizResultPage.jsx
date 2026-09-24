import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { api } from "../api/apiClient";

function formatTime(seconds) {
  if (seconds == null || !Number.isFinite(Number(seconds))) return "—";
  const s = Math.max(0, Number(seconds));
  return `${Math.floor(s / 60)}m ${String(s % 60).padStart(2, "0")}s`;
}

function csvCell(value) { return `"${String(value ?? "").replaceAll('"', '""')}"`; }

export default function QuizResultPage(){
  const {id}=useParams();
  const navigate=useNavigate();
  const location=useLocation();
  const [result,setResult]=useState(null); const [board,setBoard]=useState([]); const [standing,setStanding]=useState(null);
  const [loading,setLoading]=useState(true); const [error,setError]=useState(""); const [lastUpdated,setLastUpdated]=useState(null);
  const [reviewOpen,setReviewOpen]=useState(true);

  async function loadBoard(showError=true){
    try { const [r,b]=await Promise.all([api.getAttempt(id),api.leaderboard(id)]); setResult(r); setBoard(b?.leaderboard||[]); setStanding(b?.myStanding||null); setLastUpdated(new Date()); if(showError)setError(""); }
    catch(e){if(showError)setError(e.message||"Unable to load result.");} finally {if(showError)setLoading(false);}
  }
  useEffect(()=>{loadBoard(true);const t=setInterval(()=>loadBoard(false),5000);return()=>clearInterval(t);},[id]);

  const review=result?.review||[];
  const stats=useMemo(()=>{
    const correct=review.filter(x=>x.correct).length, unanswered=review.filter(x=>!x.givenAnswer).length, wrong=Math.max(0,review.length-correct-unanswered);
    const byDifficulty={}; const byType={};
    review.forEach(x=>{ const key=x.difficulty||"MEDIUM"; byDifficulty[key]=(byDifficulty[key]||{total:0,correct:0}); byDifficulty[key].total++; if(x.correct)byDifficulty[key].correct++; const type=x.type||"QUESTION"; byType[type]=(byType[type]||{total:0,correct:0}); byType[type].total++; if(x.correct)byType[type].correct++; });
    return {correct,wrong,unanswered,byDifficulty,byType};
  },[review]);
  const pct=result?.maxScore?Math.max(0,Math.round(result.score/result.maxScore*100)):0;
  const used=result?.attemptsUsed||1; const canRetake=result?.allowRetake===true && used < (result?.maxAttempts||1);

  function downloadReport(){
    const rows=[["Quiz","Question","Your Answer","Correct Answer","Result","Time Spent (seconds)","Explanation"]];
    review.forEach((x,i)=>rows.push([result.title,`Question ${i+1}: ${x.question}`,x.givenAnswer||"Unanswered",x.correctAnswer||"",x.correct?"Correct":x.givenAnswer?"Wrong":"Unanswered",result.questionTimeSeconds?.[i]||0,x.explanation||""]));
    const summary=[["Score",result.score],["Maximum Score",result.maxScore],["Percentage",`${pct}%`],["Rank",standing?.rank??""],["Percentile",standing?.percentile?`${standing.percentile}%`:""],["Total Time",formatTime(standing?.timeTakenSeconds)]];
    const csv=[...summary.map(r=>r.map(csvCell).join(",")),"",...rows.map(r=>r.map(csvCell).join(","))].join("\n");
    const blob=new Blob([csv],{type:"text/csv;charset=utf-8"}); const url=URL.createObjectURL(blob); const a=document.createElement("a"); a.href=url; a.download=`quizora-${(result.title||"quiz").replace(/[^a-z0-9]+/gi,"-").toLowerCase()}-result.csv`; a.click(); URL.revokeObjectURL(url);
  }

  if(loading)return <div className="public-page"><main className="listing-page"><div className="empty-panel"><h2>Preparing your detailed result…</h2></div></main></div>;
  if(error)return <div className="public-page"><main className="listing-page"><div className="alert error">{error}</div></main></div>;
  return <div className="public-page"><main className="listing-page">
    <div className="result-hero"><span className="eyebrow">QUIZ COMPLETED</span><h1>{result.title}</h1><div className="result-score"><strong>{result.score}</strong><span>/ {result.maxScore ?? result.totalQuestions}</span></div><p>You answered {result.correctAnswers} of {result.totalQuestions} questions correctly.</p><small>{result.maxScore ?? result.totalQuestions} maximum points{result.expired?" · Time expired":""} · Attempt {used}{result.maxAttempts>1?` of ${result.maxAttempts}`:""}</small><div className="result-percent">{pct}%</div></div>
    {standing&&<div className="standing-card"><div><span>YOUR RANK</span><strong>#{standing.rank}</strong></div><div><span>PERCENTILE</span><strong>{standing.percentile}%</strong></div><div><span>TIME</span><strong>{formatTime(standing.timeTakenSeconds)}</strong></div><div><span>SCORE</span><strong>{standing.score}/{standing.maxScore??standing.total}</strong></div></div>}
    {result?.prizeDescription && <div className="table-panel achievement-banner"><div><span className="eyebrow">RECOGNITION</span><h2>{result.prizeDescription}</h2><p>Recognition configured by the competition host.</p></div></div>}
    <div className="result-actions"><Link className="button-primary" to="/dashboard">My dashboard</Link><button className="button-ghost dark" onClick={downloadReport}>Download result CSV</button>{result?.certificateEnabled && result?.resultPublished !== false && standing?.rank && standing.rank <= (result?.certificateTopRanks || 3) && <Link className="button-ghost dark" to={`/quiz/${id}/certificate`}>View certificate</Link>}{canRetake&&<button className="button-ghost dark" onClick={()=>navigate(`/quiz/${id}/start`)}>Retake quiz</button>}<Link className="button-ghost dark" to="/quizzes">Explore more quizzes</Link></div>

    <div className="result-grid">
      <section className="table-panel"><div className="table-head"><div><h2>Performance overview</h2><p>Question-level accuracy and observed time spent.</p></div></div><div className="result-stat-grid"><div><span>Correct</span><strong>{stats.correct}</strong></div><div><span>Wrong</span><strong>{stats.wrong}</strong></div><div><span>Unanswered</span><strong>{stats.unanswered}</strong></div><div><span>Accuracy</span><strong>{review.length?Math.round(stats.correct/review.length*100):0}%</strong></div></div><div className="result-chart"><div className="chart-row"><span>Correct</span><div><i style={{width:`${review.length?stats.correct/review.length*100:0}%`}}/></div><b>{stats.correct}</b></div><div className="chart-row"><span>Wrong</span><div><i style={{width:`${review.length?stats.wrong/review.length*100:0}%`}}/></div><b>{stats.wrong}</b></div><div className="chart-row"><span>Unanswered</span><div><i style={{width:`${review.length?stats.unanswered/review.length*100:0}%`}}/></div><b>{stats.unanswered}</b></div></div></section>
      <section className="table-panel"><div className="table-head"><div><h2>Practice focus</h2><p>Areas where your result shows more incorrect answers.</p></div></div>{Object.entries(stats.byDifficulty).map(([k,v])=><div className="practice-row" key={k}><strong>{k}</strong><span>{v.correct}/{v.total} correct</span><b>{Math.round(v.correct/v.total*100)}%</b></div>)}{Object.keys(stats.byDifficulty).length===0&&<div className="table-empty">No practice data available.</div>}<div className="practice-note">Use these areas as a guide for future practice. Question Bank content remains private to its host.</div></section>
    </div>

    <section className="table-panel review-panel"><div className="table-head"><div><h2>Question-by-question review</h2><p>Correct answers and explanations are available after submission.</p></div><button className="button-ghost dark small" onClick={()=>setReviewOpen(v=>!v)}>{reviewOpen?"Collapse":"Expand"}</button></div>{reviewOpen&&review.map((x,i)=><article className={`review-card ${x.correct?"review-correct":x.givenAnswer?"review-wrong":"review-unanswered"}`} key={i}><div className="review-card-head"><strong>Question {i+1}</strong><span>{x.correct?"CORRECT":x.givenAnswer?"WRONG":"UNANSWERED"} · {formatTime(result.questionTimeSeconds?.[i]||0)}</span></div><h3>{x.question}</h3>{x.imageUrl&&<img className="competition-question-image" src={x.imageUrl} alt="Question"/>}<div className="review-answer-grid"><div><span>Your answer</span><strong>{x.givenAnswer||"Unanswered"}</strong></div><div><span>Correct answer</span><strong>{x.correctAnswer||"—"}</strong></div></div>{x.explanation&&<div className="review-explanation"><strong>Explanation</strong><p>{x.explanation}</p></div>}</article>)}</section>

    <div className="table-panel leaderboard-panel"><div className="table-head"><div><h2>Live leaderboard</h2><p>Submitted results update automatically every 5 seconds.</p></div><span className="live-dot">● LIVE</span></div>{lastUpdated&&<div className="leaderboard-updated">Last updated {lastUpdated.toLocaleTimeString()}</div>}{!board.length?<div className="table-empty"><h3>No leaderboard entries yet</h3></div>:<div className="host-list">{board.map(row=><div className={`host-row ${standing?.rank===row.rank&&standing?.score===row.score?"leaderboard-self":""}`} key={`${row.participantId}-${row.rank}`}><div className="host-info"><strong>#{row.rank} Participant</strong><span>{row.score}/{row.total} points · {row.percentage}% · {formatTime(row.timeTakenSeconds)}</span></div><span className="status-pill approved">{standing?.rank===row.rank&&standing?.score===row.score?"YOU":"COMPLETED"}</span></div>)}</div>}</div>
    {location.state?.autoSubmitted&&<div className="alert success">The quiz timer reached zero, so your attempt was submitted automatically.</div>}
  </main></div>;
}
