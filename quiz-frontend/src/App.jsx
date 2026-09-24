import { Navigate, Route, Routes } from "react-router-dom";
import LandingPage from "./pages/LandingPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ParticipantDashboard from "./pages/ParticipantDashboard";
import HostDashboard from "./pages/HostDashboard";
import AdminDashboard from "./pages/AdminDashboard";
import AdminEditQuizPage from "./pages/AdminEditQuizPage";
import AdminAnalyticsPage from "./pages/AdminAnalyticsPage";
import AdminAuditPage from "./pages/AdminAuditPage";
import QuestionBankPage from "./pages/QuestionBankPage";
import ProfilePage from "./pages/ProfilePage";
import QuizListPage from "./pages/QuizListPage";
import CreateQuizPage from "./pages/CreateQuizPage";
import UnauthorizedPage from "./pages/UnauthorizedPage";
import NotFoundPage from "./pages/NotFoundPage";
import HelpPage from "./pages/HelpPage";
import EditQuizPage from "./pages/EditQuizPage";
import HostQuizDetailsPage from "./pages/HostQuizDetailsPage";
import HostQuizResultsPage from "./pages/HostQuizResultsPage";
import HostQuizMonitorPage from "./pages/HostQuizMonitorPage";
import HostQuizAnalyticsPage from "./pages/HostQuizAnalyticsPage";
import ParticipantHistoryPage from "./pages/ParticipantHistoryPage";
import ParticipantAchievementsPage from "./pages/ParticipantAchievementsPage";
import PublicQuizDetailsPage from "./pages/PublicQuizDetailsPage";
import PublicQuizResultsPage from "./pages/PublicQuizResultsPage";
import QuizAttemptPage from "./pages/QuizAttemptPage";
import QuizResultPage from "./pages/QuizResultPage";
import QuizCertificatePage from "./pages/QuizCertificatePage";
import NotificationsPage from "./pages/NotificationsPage";
import ProtectedRoute from "./components/ProtectedRoute";
import DashboardLayout from "./layouts/DashboardLayout";

export default function App() {
  return (
    <div className="quizora-app">
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/quizzes" element={<QuizListPage />} />
      <Route path="/quizzes/:id" element={<PublicQuizDetailsPage />} />
      <Route path="/quizzes/:id/results" element={<PublicQuizResultsPage />} />
      <Route path="/unauthorized" element={<UnauthorizedPage />} />

      <Route element={<ProtectedRoute roles={["PARTICIPANT", "HOST", "ADMIN"]} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/dashboard" element={<ParticipantDashboard />} />
          <Route path="/quiz/:id/start" element={<QuizAttemptPage />} />
          <Route path="/quiz/:id/result" element={<QuizResultPage />} />
          <Route path="/quiz/:id/certificate" element={<QuizCertificatePage />} />
          <Route path="/history" element={<ParticipantHistoryPage />} />
          <Route path="/achievements" element={<ParticipantAchievementsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/help" element={<HelpPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={["HOST"]} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/host" element={<HostDashboard />} />
          <Route path="/host/create" element={<CreateQuizPage />} />
          <Route path="/host/question-bank" element={<QuestionBankPage />} />
          <Route path="/host/quizzes/:id" element={<HostQuizDetailsPage />} />
          <Route path="/host/quizzes/:id/edit" element={<EditQuizPage />} />
          <Route path="/host/quizzes/:id/results" element={<HostQuizResultsPage />} />
          <Route path="/host/quizzes/:id/monitor" element={<HostQuizMonitorPage />} />
          <Route path="/host/quizzes/:id/analytics" element={<HostQuizAnalyticsPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
          <Route path="/admin/audit" element={<AdminAuditPage />} />
          <Route path="/admin/quizzes/:id/edit" element={<AdminEditQuizPage />} />
        </Route>
      </Route>

      <Route path="/not-found" element={<NotFoundPage />} />
      <Route path="*" element={<Navigate to="/not-found" replace />} />
    </Routes>
    </div>
  );
}
