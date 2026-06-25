import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { HashRouter } from 'react-router-dom';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';
import App from '@/App';

function renderApp() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <HashRouter>
          <App />
        </HashRouter>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

describe('App', () => {
  it('renders without crashing', () => {
    const { container } = renderApp();
    expect(container).toBeTruthy();
  });

  it('shows loading fallback on initial render (lazy routes)', () => {
    renderApp();
    // The page loader with Loader2 icon should be visible while lazy routes load
    const loader = document.querySelector('.animate-spin');
    expect(loader).toBeInTheDocument();
  });
});
