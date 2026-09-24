import React from "react";

export default class ErrorBoundary extends React.Component {
  state = { hasError: false, message: "" };
  static getDerivedStateFromError(error) { return { hasError: true, message: error?.message || "Unexpected application error" }; }
  componentDidCatch(error) { console.error("Quizora UI error", error); }
  render() {
    if (!this.state.hasError) return this.props.children;
    return (
      <div className="error-boundary">
        <div className="card error-boundary-card">
          <h2>Something went wrong</h2>
          <p>The application encountered an unexpected error.</p>
          <button className="primary-btn" onClick={() => window.location.reload()}>Reload Quizora</button>
          {import.meta.env.DEV && this.state.message ? <small>{this.state.message}</small> : null}
        </div>
      </div>
    );
  }
}
