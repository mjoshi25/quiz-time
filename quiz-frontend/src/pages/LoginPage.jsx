import Brand from "../components/Brand";
import { useState } from "react";
import { useLocation } from "react-router-dom";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api/apiClient";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { saveSession } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const returnTo = location.state?.from || "/dashboard";
  const [form, setForm] = useState({ email: "admin@quizapp.com", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function updateField(field, value) {
    setForm((p) => ({ ...p, [field]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const data = await api.login(form);
      saveSession(data);
      const role = data?.role;
      if (role === "ADMIN") navigate("/admin", { replace: true });
      else if (role === "HOST") navigate("/host", { replace: true });
      else navigate(returnTo.startsWith("/") ? returnTo : "/dashboard", { replace: true });
    } catch (err) {
      console.error("Login error:", err);
      setError(err?.message || "Login failed. Please check your credentials.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <Brand className="auth-brand" />
      <div className="auth-layout">
        <div className="auth-promo">
          <span className="eyebrow">YOUR NEXT CHALLENGE STARTS HERE</span>
          <h1>Knowledge is<br />power.<br /><span>Competition makes it fun.</span></h1>
          <p>Track your progress, meet fellow quiz lovers, and enjoy meaningful challenges.</p>
        </div>
        <div className="auth-panel">
          <div className="auth-panel-head">
            <span className="mini-label">QUIZORA ACCOUNT</span>
            <h2>Welcome back</h2>
            <p>Sign in to continue your quiz journey.</p>{location.state?.from && <div className="alert success">Sign in to continue to your selected competition.</div>}
          </div>
          <form onSubmit={submit}>
            <label>Email address<input type="email" required autoComplete="email" value={form.email} onChange={(e) => updateField("email", e.target.value)} /></label>
            <label>Password<input type="password" required autoComplete="current-password" value={form.password} onChange={(e) => updateField("password", e.target.value)} /></label>
            {error && <div className="alert error">{error}</div>}
            <button type="submit" className="button-primary full" disabled={busy}>{busy ? "Signing in…" : "Sign in"}<span>→</span></button>
          </form>
          <p className="auth-footer">New to Quizora? <Link to="/register">Create an account</Link></p>
        </div>
      </div>
    </div>
  );
}
