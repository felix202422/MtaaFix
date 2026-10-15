import { render } from '@testing-library/react';
import { MemoryRouter, useRoutes } from 'react-router-dom';
import { routes } from '../App';

export function renderRoute(path: string) {
  function RouteElement() {
    return useRoutes(routes);
  }
  return render(
    <MemoryRouter initialEntries={[path]}>
      <RouteElement />
    </MemoryRouter>,
  );
}
