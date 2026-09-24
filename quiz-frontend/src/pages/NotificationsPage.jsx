import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/apiClient";

function fmt(value){ try{return value?new Date(value).toLocaleString():"";}catch{return value||"";} }

export default function NotificationsPage(){
  const [items,setItems]=useState([]); const [busy,setBusy]=useState(false); const [error,setError]=useState("");
  async function load(){try{setError("");setItems(await api.notifications()||[])}catch(e){setError(e.message||"Unable to load notifications.")}}
  useEffect(()=>{load()},[]);
  async function read(id){try{await api.markNotificationRead(id);setItems(x=>x.map(n=>n.id===id?{...n,read:true}:n))}catch(e){setError(e.message)} }
  async function readAll(){setBusy(true);try{await api.markAllNotificationsRead();setItems(x=>x.map(n=>({...n,read:true})))}catch(e){setError(e.message)}finally{setBusy(false)}}
  const unread=items.filter(n=>!n.read).length;
  return <div className="dashboard-form-page">
    <div className="form-page-top"><div><span className="eyebrow">QUIZORA ALERTS</span><h1>Notifications</h1><p>Important updates about your account, competitions and results.</p></div><div className="form-actions-inline"><button className="button-ghost dark small" onClick={load}>Refresh</button>{unread>0&&<button className="button-primary small" disabled={busy} onClick={readAll}>{busy?"Updating…":"Mark all read"}</button>}</div></div>
    {error&&<div className="alert error">{error}</div>}
    <div className="table-panel notification-panel">
      {!items.length?<div className="table-empty"><div className="empty-icon">🔔</div><h3>No notifications yet</h3><p>Quizora will show important competition and account updates here.</p></div>:
      <div className="notification-list">{items.map(n=><div key={n.id} className={`notification-row ${n.read?"read":"unread"}`}><div className={`notification-icon notification-${(n.type||"INFO").toLowerCase()}`}>🔔</div><div className="notification-content"><div className="notification-title"><strong>{n.title}</strong>{!n.read&&<span className="status-pill approved">NEW</span>}</div><p>{n.message}</p><small>{fmt(n.createdAt)}</small>{n.link&&<Link className="text-link" to={n.link}>Open →</Link>}</div>{!n.read&&<button className="button-ghost dark small" onClick={()=>read(n.id)}>Mark read</button>}</div>)}</div>}
    </div>
  </div>
}
