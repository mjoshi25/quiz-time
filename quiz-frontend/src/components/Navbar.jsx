import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import Brand from "./Brand";

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  function out() { logout(); navigate("/"); setOpen(false); }
  const dashboard = user?.role === "ADMIN" ? "/admin" : user?.role === "HOST" ? "/host" : "/dashboard";
  return <header className="navbar">
    <Brand />
    <button className="mobile-toggle" onClick={() => setOpen(!open)}>☰</button>
    <nav className={`nav-links ${open ? "open" : ""}`}>
      <Link to="/quizzes" onClick={() => setOpen(false)}>Explore quizzes</Link>
      <a href="/#features" onClick={() => setOpen(false)}>Features</a>
      <a href="/#how" onClick={() => setOpen(false)}>How it works</a>
      {user ? <><Link to={dashboard} onClick={() => setOpen(false)}>Dashboard</Link>{user.role === "HOST" && <Link to="/host/create" onClick={() => setOpen(false)}>Create quiz</Link>}<button className="nav-outline" onClick={out}>Sign out</button></> : <><Link to="/login" onClick={() => setOpen(false)}>Sign in</Link><Link className="nav-primary" to="/register" onClick={() => setOpen(false)}>Get started</Link></>}
    </nav>
  </header>;
}
