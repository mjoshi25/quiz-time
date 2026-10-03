import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import Brand from "./Brand";

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const dashboard = user?.role === "ADMIN" ? "/admin" : user?.role === "HOST" ? "/host" : "/dashboard";

  function closeMenu() { setOpen(false); }
  function out() { logout(); navigate("/"); closeMenu(); }

  const links = (mobile = false) => (
    <>
      <Link to="/quizzes" onClick={closeMenu}>Explore quizzes</Link>
      <a href="/#features" onClick={closeMenu}>Features</a>
      <a href="/#how" onClick={closeMenu}>How it works</a>
      {user ? (
        <>
          <Link to={dashboard} onClick={closeMenu}>Dashboard</Link>
          {user.role === "HOST" && <Link to="/host/create" onClick={closeMenu}>Create quiz</Link>}
          <button className="nav-outline" onClick={out}>Sign out</button>
        </>
      ) : (
        <>
          <Link to="/login" onClick={closeMenu}>Sign in</Link>
          <Link className="nav-primary" to="/register" onClick={closeMenu}>Get started</Link>
        </>
      )}
    </>
  );

  return (
    <>
      <header className="navbar">
        <Brand />
        <button
          type="button"
          className="mobile-toggle"
          aria-label={open ? "Close menu" : "Open menu"}
          aria-expanded={open}
          onClick={() => setOpen((value) => !value)}
        >
          <span className={open ? "menu-icon menu-icon-close" : "menu-icon menu-icon-open"} aria-hidden="true">{open ? "×" : "☰"}</span>
        </button>
        <nav className="nav-links" aria-label="Primary navigation">{links()}</nav>
      </header>

      {open && (
        <div className="mobile-menu" role="dialog" aria-label="Mobile navigation">
          <nav className="mobile-menu-links">{links(true)}</nav>
        </div>
      )}
    </>
  );
}
