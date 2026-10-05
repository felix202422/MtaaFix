import { Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import { useAuth } from './context/AuthContext';
import { ROLES } from './utils/constants';
import RoleRoute from './components/RoleRoute';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import PortalDashboard from './components/portal/PortalDashboard';
import StudentsPage from './pages/StudentsPage';
import TeachersPage from './pages/TeachersPage';
import ClassesPage from './pages/ClassesPage';
import AttendancePage from './pages/AttendancePage';
import ExaminationsPage from './pages/ExaminationsPage';
import AssignmentsPage from './pages/AssignmentsPage';
import TimetablePage from './pages/TimetablePage';
import SubjectsPage from './pages/SubjectsPage';
import FeesPage from './pages/FeesPage';
import LibraryPage from './pages/LibraryPage';
import SettingsPage from './pages/SettingsPage';
import EventsPage from './pages/EventsPage';
import InventoryPage from './pages/InventoryPage';
import TransportPage from './pages/TransportPage';
import HostelPage from './pages/HostelPage';

export default function App() {
  const { user } = useAuth();
  const isAdmin = user?.role === ROLES.SUPER_ADMIN || user?.role === ROLES.SCHOOL_ADMIN;

  return (
    <>
      <ToastContainer position="top-right" autoClose={3000} />
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        {/* Admins get the chart dashboard; other roles get their portal */}
        <Route
          path="/"
          element={
            <RoleRoute>
              {isAdmin ? <DashboardPage /> : <PortalDashboard />}
            </RoleRoute>
          }
        />

        <Route path="/students" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.TEACHER, ROLES.REGISTRAR, ROLES.RECEPTIONIST]}><StudentsPage /></RoleRoute>} />
        <Route path="/teachers" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.REGISTRAR]}><TeachersPage /></RoleRoute>} />
        <Route path="/classes" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.TEACHER, ROLES.REGISTRAR]}><ClassesPage /></RoleRoute>} />
        <Route path="/attendance" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.TEACHER, ROLES.REGISTRAR]}><AttendancePage /></RoleRoute>} />
        <Route path="/examinations" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.TEACHER, ROLES.STUDENT, ROLES.PARENT]}><ExaminationsPage /></RoleRoute>} />
        <Route path="/assignments" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.TEACHER, ROLES.STUDENT, ROLES.PARENT]}><AssignmentsPage /></RoleRoute>} />
        <Route path="/timetable" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.TEACHER, ROLES.STUDENT, ROLES.PARENT]}><TimetablePage /></RoleRoute>} />
        <Route path="/subjects" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.TEACHER, ROLES.STUDENT]}><SubjectsPage /></RoleRoute>} />
        <Route path="/fees" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.ACCOUNTANT]}><FeesPage /></RoleRoute>} />
        <Route path="/library" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, ROLES.LIBRARIAN, ROLES.TEACHER, ROLES.STUDENT]}><LibraryPage /></RoleRoute>} />
        <Route path="/settings" element={<RoleRoute><SettingsPage /></RoleRoute>} />
        <Route path="/events" element={<RoleRoute><EventsPage /></RoleRoute>} />
        <Route path="/inventory" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, 'INVENTORY_MANAGER']}><InventoryPage /></RoleRoute>} />
        <Route path="/transport" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, 'TRANSPORT_MANAGER']}><TransportPage /></RoleRoute>} />
        <Route path="/hostel" element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.SCHOOL_ADMIN, 'HOSTEL_MANAGER']}><HostelPage /></RoleRoute>} />

        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </>
  );
}
