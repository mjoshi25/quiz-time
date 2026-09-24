import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Sidebar() {
  const { user } = useAuth();
  const dashboard = user?.role === "ADMIN" ? "/admin" : user?.role === "HOST" ? "/host" : "/dashboard";
  const initials = (user?.name || "U").trim().charAt(0).toUpperCase();
  return <aside className="sidebar">
    <div className="side-profile">
      {user?.profilePhoto ? <img className="avatar avatar-photo" src={user.profilePhoto} alt="Profile" /> : <div className="avatar">{initials}</div>}
      <div><strong>{user?.name || "User"}</strong><span>{user?.role || "USER"}</span></div>
    </div>
    <div className="side-label">Workspace</div>
    <NavLink to={dashboard} className="side-link">⌂ <span>Dashboard</span></NavLink>
    {user?.role === "PARTICIPANT" && <><NavLink to="/quizzes" className="side-link">◇ <span>Explore quizzes</span></NavLink><NavLink to="/history" className="side-link">▥ <span>Quiz history</span></NavLink><NavLink to="/achievements" className="side-link">★ <span>Achievements</span></NavLink></>}
    {user?.role === "HOST" && <><NavLink to="/host" className="side-link">✦ <span>Host centre</span></NavLink><NavLink to="/host/create" className="side-link">＋ <span>Create quiz</span></NavLink><NavLink to="/host/question-bank" className="side-link">◇ <span>Question bank</span></NavLink><NavLink to="/host" className="side-link">▤ <span>Results</span></NavLink></>}
    {user?.role === "ADMIN" && <><NavLink to="/admin" className="side-link">♙ <span>Admin console</span></NavLink></>}
    <NavLink to="/notifications" className="side-link">🔔 <span>Notifications</span></NavLink>
    <NavLink to="/profile" className="side-link">◉ <span>Profile</span></NavLink>
    <NavLink to="/help" className="side-help-link"><div className="side-help"><b>Need help?</b><small>Open the Quizora Help Center and contact support.</small><span>Open help →</span></div></NavLink>
  </aside>;
}
