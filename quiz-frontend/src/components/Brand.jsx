import { Link } from "react-router-dom";

export default function Brand({ compact = false, className = "", to = "/" }) {
  return (
    <Link className={`brand ${compact ? "compact" : ""} ${className}`.trim()} to={to} aria-label="asmj-Quizora home">
      <span className="brand-logo-wrap" aria-hidden="true">
        <img src="/asmj-quizora-logo.svg" alt="" className="brand-logo" />
      </span>
      <span className="brand-name"><b>asmj</b>-Quizora</span>
    </Link>
  );
}
