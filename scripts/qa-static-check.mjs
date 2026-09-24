import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const requiredFiles = [
  'backend/pom.xml',
  'backend/src/main/java/com/quizapp/config/SecurityConfig.java',
  'backend/src/main/java/com/quizapp/security/HostAuthorization.java',
  'backend/src/main/java/com/quizapp/controller/AuthController.java',
  'backend/src/main/java/com/quizapp/controller/QuizController.java',
  'backend/src/main/java/com/quizapp/controller/QuizAttemptController.java',
  'backend/src/main/java/com/quizapp/controller/PublicQuizController.java',
  'backend/src/main/java/com/quizapp/controller/ParticipantProfileController.java',
  'backend/src/main/java/com/quizapp/controller/AdminAuditController.java',
  'backend/src/main/java/com/quizapp/controller/AdminAnalyticsController.java',
  'frontend/package.json',
  'frontend/src/App.jsx',
  'frontend/src/api/apiClient.js',
  'frontend/src/components/ProtectedRoute.jsx'
];
const checks = [
  ['Host approval is DB-backed', 'backend/src/main/java/com/quizapp/security/HostAuthorization.java', 'getHostApprovalStatus()'],
  ['Admin API role protection', 'backend/src/main/java/com/quizapp/config/SecurityConfig.java', 'requestMatchers("/api/admin/**").hasRole("ADMIN")'],
  ['Host API role protection', 'backend/src/main/java/com/quizapp/config/SecurityConfig.java', 'requestMatchers("/api/host/**").hasRole("HOST")'],
  ['Participant API role protection', 'backend/src/main/java/com/quizapp/config/SecurityConfig.java', 'requestMatchers("/api/participant/**").hasRole("PARTICIPANT")'],
  ['Public API explicitly separated', 'backend/src/main/java/com/quizapp/config/SecurityConfig.java', '"/api/public/**"'],
  ['Quiz lifecycle controller exists', 'backend/src/main/java/com/quizapp/controller/QuizController.java', '@PatchMapping("/{id}/close")'],
  ['Attempt submission exists', 'backend/src/main/java/com/quizapp/controller/QuizAttemptController.java', '@PostMapping("/{quizId}/submit")'],
  ['Public results endpoint exists', 'backend/src/main/java/com/quizapp/controller/PublicQuizController.java', '@GetMapping("/{id}/results")'],
  ['Participant achievements API exists', 'backend/src/main/java/com/quizapp/controller/ParticipantProfileController.java', '@GetMapping'],
  ['Admin audit export exists', 'backend/src/main/java/com/quizapp/controller/AdminAuditController.java', 'produces="text/csv"'],
  ['Admin analytics range exists', 'backend/src/main/java/com/quizapp/controller/AdminAnalyticsController.java', '@GetMapping("/range")'],
  ['Frontend public results route exists', 'frontend/src/App.jsx', '/quizzes/:id/results'],
  ['Frontend achievements route exists', 'frontend/src/App.jsx', '/achievements'],
  ['Frontend admin audit route exists', 'frontend/src/App.jsx', '/admin/audit'],
  ['Frontend host monitor route exists', 'frontend/src/App.jsx', '/host/quizzes/:id/monitor']
];

let failed = 0;
for (const file of requiredFiles) {
  if (!fs.existsSync(path.join(root, file))) {
    console.error(`FAIL  missing: ${file}`);
    failed++;
  } else {
    console.log(`PASS  file: ${file}`);
  }
}
for (const [label, file, needle] of checks) {
  const full = path.join(root, file);
  if (!fs.existsSync(full) || !fs.readFileSync(full, 'utf8').includes(needle)) {
    console.error(`FAIL  ${label}`);
    failed++;
  } else {
    console.log(`PASS  ${label}`);
  }
}
const pkg = JSON.parse(fs.readFileSync(path.join(root, 'frontend/package.json'), 'utf8'));
for (const script of ['dev', 'build']) {
  if (!pkg.scripts?.[script]) {
    console.error(`FAIL  frontend npm script: ${script}`);
    failed++;
  } else console.log(`PASS  frontend npm script: ${script}`);
}
if (failed) {
  console.error(`\nQA static checks failed: ${failed}`);
  process.exit(1);
}
console.log('\nQA static checks passed. Runtime/integration tests still require MongoDB and the application services.');
