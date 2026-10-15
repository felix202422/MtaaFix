import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import {
  api,
  clearAuthTokens,
  getAccessToken,
  getRefreshToken,
  setAuthTokens,
  setSessionExpiredHandler,
} from '../lib/api';
import type { User } from '../lib/types';

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (input: {
    email: string;
    password: string;
    fullName: string;
    mobile?: string;
  }) => Promise<User>;
  logout: () => Promise<void>;
  updateUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(() => getAccessToken() !== null);

  // Restore the session on mount if a token exists.
  useEffect(() => {
    let cancelled = false;
    if (!getAccessToken() && !getRefreshToken()) {
      setLoading(false);
      return;
    }
    api
      .getMe()
      .then((me) => {
        if (!cancelled) setUser(me);
      })
      .catch(() => {
        if (!cancelled) {
          clearAuthTokens();
          setUser(null);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // If silent refresh fails, the API layer clears tokens and we drop the session.
  useEffect(() => {
    setSessionExpiredHandler(() => setUser(null));
    return () => setSessionExpiredHandler(null);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const auth = await api.login(email, password);
    setAuthTokens(auth.accessToken, auth.refreshToken);
    setUser(auth.user);
    return auth.user;
  }, []);

  const register = useCallback(
    async (input: { email: string; password: string; fullName: string; mobile?: string }) => {
      const created = await api.register(input);
      return created;
    },
    [],
  );

  const logout = useCallback(async () => {
    try {
      if (getAccessToken()) await api.logout();
    } catch {
      // Ignore network errors on logout — clear locally regardless.
    }
    clearAuthTokens();
    setUser(null);
  }, []);

  const updateUser = useCallback((next: User) => setUser(next), []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      loading,
      login,
      register,
      logout,
      updateUser,
    }),
    [user, loading, login, register, logout, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
