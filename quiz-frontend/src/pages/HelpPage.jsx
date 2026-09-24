import { useState } from "react";

const faqs = [
  ["How do I join a quiz?", "Open Explore quizzes, choose a published competition and use the Join option when participation is enabled."],
  ["How do I become a host?", "Register as a Host. Your account starts as PENDING and an administrator must approve it before host-management APIs become available."],
  ["Why can't I create a quiz?", "Only an approved, active host can create and publish quizzes. Check your Profile page for the current host approval status."],
  ["How do I change my profile photo?", "Open Profile, choose Change photo, select an image below 2 MB and press Save changes."],
  ["Where can I report a problem?", "Use the Contact support button below and include the page, action and error message you experienced."]
];

export default function HelpPage() {
  const [open, setOpen] = useState(0);
  const supportEmail = import.meta.env.VITE_SUPPORT_EMAIL || "support@quizora.com";
  const subject = encodeURIComponent("Quizora support request");
  const body = encodeURIComponent("Hello Quizora Support,\n\nI need help with: \nPage: \nAction: \nError/message: \n\nThanks.");

  return <div>
    <div className="page-heading"><div><span className="eyebrow">QUIZORA SUPPORT</span><h1>Need help?</h1><p>Find quick answers or contact the support team.</p></div></div>
    <div className="help-grid">
      <section className="help-panel">
        <div className="section-title-row"><div><h2>Frequently asked questions</h2><p>Quick answers for common account and quiz actions.</p></div></div>
        <div className="faq-list">
          {faqs.map(([question, answer], index) => <div className={`faq-item ${open === index ? "open" : ""}`} key={question}>
            <button type="button" onClick={() => setOpen(open === index ? -1 : index)}><span>{question}</span><b>{open === index ? "−" : "+"}</b></button>
            {open === index && <p>{answer}</p>}
          </div>)}
        </div>
      </section>
      <aside className="help-contact">
        <div className="help-icon">?</div>
        <h2>Still need help?</h2>
        <p>Send the support team the page and error details so the issue can be investigated quickly.</p>
        <a className="button-primary full" href={`mailto:${supportEmail}?subject=${subject}&body=${body}`}>Contact support <span>→</span></a>
        <small>Support email: {supportEmail}</small>
      </aside>
    </div>
  </div>;
}
