import { createBrowserRouter, RouterProvider, type RouteObject } from 'react-router-dom';
import { ToastProvider } from './design-system';
import { Layout } from './app/Layout';
import { DesignGallery } from './gallery/DesignGallery';
import { ExplorePage } from './pages/ExplorePage';
import { HowItWorksPage } from './pages/HowItWorksPage';
import { LandingPage } from './pages/LandingPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { PublicReportDetailPage } from './pages/PublicReportDetailPage';
import { ReportInfoPage } from './pages/ReportInfoPage';

export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { path: '/', element: <LandingPage /> },
      { path: '/how-it-works', element: <HowItWorksPage /> },
      { path: '/explore', element: <ExplorePage /> },
      { path: '/reports/:code', element: <PublicReportDetailPage /> },
      { path: '/report', element: <ReportInfoPage /> },
      { path: '/design', element: <DesignGallery /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];

const router = createBrowserRouter(routes);

export default function App() {
  return (
    <ToastProvider>
      <RouterProvider router={router} />
    </ToastProvider>
  );
}
