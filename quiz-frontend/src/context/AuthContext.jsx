import { createContext, useContext, useMemo, useState } from "react";

const AuthContext = createContext(null);

function readUser() {
  try {
    return JSON.parse(localStorage.getItem("quizora_user") || "null");
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);

  function saveSession(data) {
    const u = {
      id: data?.id,
      name: data?.name,
      email: data?.email,
      role: data?.role,
      hostApprovalStatus: data?.hostApprovalStatus,
      accountStatus: data?.accountStatus,
      profilePhoto: data?.profilePhoto || null
    };

    if (data?.token) localStorage.setItem("quizora_token", data.token);
    localStorage.setItem("quizora_user", JSON.stringify(u));
    setUser(u);
  }

  // Backward-compatible alias for older pages.
  const save = saveSession;

  function updateUser(patch) {
    setUser((current) => {
      const next = { ...current, ...patch };
      localStorage.setItem("quizora_user", JSON.stringify(next));
      return next;
    });
  }

  function logout() {
    localStorage.removeItem("quizora_token");
    localStorage.removeItem("quizora_user");
    setUser(null);
  }

  const value = useMemo(() => ({
    user,
    isAuthenticated: !!(user && localStorage.getItem("quizora_token")),
    saveSession,
    save,
    updateUser,
    logout
  }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
