import { useEffect, useRef, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { api } from "../api/apiClient";

export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const [form, setForm] = useState({ name: user?.name || "", profilePhoto: user?.profilePhoto || "" });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState("");
  const fileRef = useRef(null);

  useEffect(() => {
    let active = true;
    api.me().then((data) => {
      if (!active) return;
      updateUser(data);
      setForm({ name: data?.name || "", profilePhoto: data?.profilePhoto || "" });
      setLoading(false);
    }).catch((e) => {
      if (active) {
        setError(e.status === 401 ? "Your session has expired. Please sign in again." : e.message || "Unable to load profile details.");
        setLoading(false);
      }
    });
    return () => { active = false; };
  }, []);

  function choosePhoto(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    setError(""); setSaved("");
    if (!file.type.startsWith("image/")) { setError("Please select an image file."); return; }
    if (file.size > 2 * 1024 * 1024) { setError("Please select an image smaller than 2 MB."); return; }
    const reader = new FileReader();
    reader.onload = () => setForm((p) => ({ ...p, profilePhoto: reader.result }));
    reader.readAsDataURL(file);
  }

  async function saveProfile(event) {
    event.preventDefault();
    setSaving(true); setError(""); setSaved("");
    try {
      const data = await api.updateProfile({ name: form.name, profilePhoto: form.profilePhoto || null });
      updateUser(data);
      setForm({ name: data.name || "", profilePhoto: data.profilePhoto || "" });
      setSaved("Profile updated successfully.");
    } catch (e) {
      setError(e.message || "Unable to update profile.");
    } finally { setSaving(false); }
  }

  const initials = (form.name || user?.name || "U").trim().charAt(0).toUpperCase();

  return <div>
    <div className="page-heading"><div><span className="eyebrow">ACCOUNT SETTINGS</span><h1>Your profile</h1><p>Manage your account information and profile photo.</p></div></div>
    {error && <div className="alert error">{error}</div>}
    {saved && <div className="alert success">{saved}</div>}
    <form className="profile-grid" onSubmit={saveProfile}>
      <section className="profile-panel profile-card-main">
        <div className="profile-photo-wrap">
          {form.profilePhoto ? <img className="profile-photo" src={form.profilePhoto} alt="Profile" /> : <div className="profile-avatar large">{initials}</div>}
          <button type="button" className="photo-button" onClick={() => fileRef.current?.click()}>Change photo</button>
          <input ref={fileRef} hidden type="file" accept="image/*" onChange={choosePhoto} />
          <small>JPG, PNG or WebP · max 2 MB</small>
        </div>
        <div><h2>{user?.name || "Your name"}</h2><p>{user?.email || "—"}</p><div className="profile-pills"><span className="status-pill">{user?.role || "—"}</span>{user?.role === "HOST" && <span className={`status-pill ${user?.hostApprovalStatus !== "APPROVED" ? "pending" : ""}`}>{user?.hostApprovalStatus || "PENDING"}</span>}</div></div>
      </section>

      <section className="details-panel profile-edit-panel">
        <h2>Personal details</h2>
        <label>Full name<input value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} required maxLength={100} /></label>
        <label>Email address<input value={user?.email || ""} disabled /></label>
        <div className="profile-detail-row"><span>ACCOUNT ROLE</span><strong>{user?.role || "—"}</strong></div>
        <div className="profile-detail-row"><span>ACCOUNT STATUS</span><strong>{user?.accountStatus || "ACTIVE"}</strong></div>
        {user?.role === "HOST" && <div className="profile-detail-row"><span>HOST APPROVAL</span><strong>{user?.hostApprovalStatus || "PENDING"}</strong></div>}
        <button className="button-primary" type="submit" disabled={saving || loading}>{saving ? "Saving…" : "Save changes"}<span>→</span></button>
      </section>
    </form>
  </div>;
}
