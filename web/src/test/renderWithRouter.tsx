import { render } from '@testing-library/react';
import { MemoryRouter, useRoutes } from 'react-router-dom';
import { routes } from '../App';
import { AuthProvider } from '../app/AuthProvider';
import { ToastProvider } from '../design-system';

export function renderRoute(path: string) {
  function RouteElement() {
    return useRoutes(routes);
  }
  return render(
    <MemoryRouter initialEntries={[path]}>
      <ToastProvider>
        <AuthProvider>
          <RouteElement />
        </AuthProvider>
      </ToastProvider>
    </MemoryRouter>,
  );
}
