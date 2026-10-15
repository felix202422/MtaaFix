import { createBrowserRouter, RouterProvider, type RouteObject } from 'react-router-dom';
import { ToastProvider } from './design-system';
import { Layout } from './app/Layout';
import { AuthProvider } from './app/AuthProvider';
import { RequireAuth } from './app/RequireAuth';
import { DesignGallery } from './gallery/DesignGallery';
import { ExplorePage } from './pages/ExplorePage';
import { HowItWorksPage } from './pages/HowItWorksPage';
import { LandingPage } from './pages/LandingPage';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { PublicReportDetailPage } from './pages/PublicReportDetailPage';
import { RegisterPage } from './pages/RegisterPage';
import { ReportInfoPage } from './pages/ReportInfoPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { AppDashboardPage } from './pages/app/AppDashboardPage';
import { MyReportsPage } from './pages/app/MyReportsPage';
import { NotificationsPage } from './pages/app/NotificationsPage';
import { ProfilePage } from './pages/app/ProfilePage';
import { ReportTrackingPage } from './pages/app/ReportTrackingPage';
import { ReportWizardPage } from './pages/app/ReportWizardPage';

export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { path: '/', element: <LandingPage /> },
      { path: '/how-it-works', element: <HowItWorksPage /> },
      { path: '/explore', element: <ExplorePage /> },
      { path: '/reports/:code', element: <PublicReportDetailPage /> },
      { path: '/report', element: <ReportInfoPage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
      {
        path: '/app',
        element: <RequireAuth />,
        children: [
          { index: true, element: <AppDashboardPage /> },
          { path: 'reports', element: <MyReportsPage /> },
          { path: 'reports/new', element: <ReportWizardPage /> },
          { path: 'reports/:id', element: <ReportTrackingPage /> },
          { path: 'notifications', element: <NotificationsPage /> },
          { path: 'profile', element: <ProfilePage /> },
        ],
      },
      { path: '/design', element: <DesignGallery /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];

const router = createBrowserRouter(routes);

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </ToastProvider>
  );
}
