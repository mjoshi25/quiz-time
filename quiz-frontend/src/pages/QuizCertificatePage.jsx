import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/apiClient";

export default function QuizCertificatePage(){
  const {id}=useParams();
  const [result,setResult]=useState(null); const [standing,setStanding]=useState(null); const [loading,setLoading]=useState(true); const [error,setError]=useState("");
  useEffect(()=>{Promise.all([api.getAttempt(id),api.leaderboard(id)]).then(([r,b])=>{setResult(r);setStanding(b?.myStanding||null);}).catch(e=>setError(e.message||"Unable to load certificate.")).finally(()=>setLoading(false));},[id]);
  if(loading)return <div className="certificate-page"><div className="empty-panel"><h2>Preparing certificate…</h2></div></div>;
  if(error)return <div className="certificate-page"><div className="alert error">{error}</div></div>;
  const eligible=result?.resultPublished!==false && result?.certificateEnabled===true && result?.status==="SUBMITTED" && standing?.rank && standing.rank <= (result.certificateTopRanks||3);
  if(!eligible)return <div className="certificate-page"><div className="empty-panel"><h2>Certificate unavailable</h2><p>This competition has not enabled a certificate for your current result.</p><Link className="button-primary" to={`/quiz/${id}/result`}>Back to result</Link></div></div>;
  return <div className="certificate-page"><div className="certificate-actions"><Link className="button-ghost dark" to={`/quiz/${id}/result`}>← Back to result</Link><button className="button-primary" onClick={()=>window.print()}>Print / Save as PDF</button></div><article className="certificate-sheet"><div className="certificate-border"><div className="certificate-mark">asmj-Quizora</div><span className="eyebrow">CERTIFICATE OF ACHIEVEMENT</span><h1>Certificate of Achievement</h1><p className="certificate-presented">This certificate is proudly presented to</p><h2>{standing.participantName || "Participant"}</h2><p>for securing <strong>Rank #{standing.rank}</strong> in</p><h3>{result.title}</h3><div className="certificate-stats"><div><span>SCORE</span><strong>{result.score}/{result.maxScore}</strong></div><div><span>PERCENTAGE</span><strong>{standing.percentage}%</strong></div><div><span>DATE</span><strong>{result.submittedAt ? new Date(result.submittedAt).toLocaleDateString() : new Date().toLocaleDateString()}</strong></div></div>{result.prizeDescription&&<p className="certificate-prize">{result.prizeDescription}</p>}<div className="certificate-footer"><span>Quizora Online Quiz Competition</span><span>Ranked result · {standing.rank} of {standing.total || "participants"}</span></div></div></article></div>;
}
