import Brand from "../components/Brand";import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api } from "../api/apiClient";
import { useAuth } from "../context/AuthContext";

export default function RegisterPage() {
  const [role, setRole] = useState("participant");
  const [f, setF] = useState({ name: "", email: "", password: "" });
  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const { save } = useAuth();
  const nav = useNavigate();
  const location = useLocation();
  const returnTo = location.state?.from || "";

  async function submit(e) {
    e.preventDefault(); setBusy(true); setErr(""); setMsg("");
    try {
      const d = role === "host" ? await api.registerHost(f) : await api.registerParticipant(f);
      save(d);
      if (role === "host") {
        setMsg("Your host account is pending admin approval.");
        setTimeout(() => nav("/host"), 600);
      } else {
        setMsg("Account created successfully. Returning to your competition…");
        setTimeout(() => nav(returnTo.startsWith("/") ? returnTo : "/dashboard", { replace: true }), 600);
      }
    } catch (x) { setErr(x.message || "Registration failed."); }
    finally { setBusy(false); }
  }

  return <div className="auth-page">
    <Brand className="auth-brand" />
    <div className="auth-layout">
      <div className="auth-promo"><span className="eyebrow">JOIN THE COMMUNITY</span><h1>Every question is<br/><span>a new possibility.</span></h1><p>Choose your account type and start your Quizora journey today.</p></div>
      <div className="auth-panel"><span className="mini-label">CREATE ACCOUNT</span><h2>Start your journey</h2><p>Registration is quick and secure.</p>
        {returnTo && <div className="alert success">Create your participant account to continue to the selected competition.</div>}
        <div className="role-tabs"><button type="button" className={role === "participant" ? "selected" : ""} onClick={() => setRole("participant")}>Participant</button><button type="button" className={role === "host" ? "selected" : ""} onClick={() => setRole("host")}>Host</button></div>
        <form onSubmit={submit}>
          <label>Full name<input required value={f.name} placeholder="Enter your full name" onChange={x => setF({ ...f, name: x.target.value })}/></label>
          <label>Email address<input required type="email" value={f.email} placeholder="you@example.com" onChange={x => setF({ ...f, email: x.target.value })}/></label>
          <label>Password<input required minLength="8" type="password" value={f.password} placeholder="Minimum 8 characters" onChange={x => setF({ ...f, password: x.target.value })}/></label>
          {err && <div className="alert error">{err}</div>}{msg && <div className="alert success">{msg}</div>}
          <button className="button-primary full" disabled={busy}>{busy ? "Creating…" : "Create account"}<span>→</span></button>
        </form>
        <p className="auth-footer">Already registered? <Link to="/login" state={returnTo ? { from: returnTo } : undefined}>Sign in here</Link></p>
      </div>
    </div>
  </div>;
}
