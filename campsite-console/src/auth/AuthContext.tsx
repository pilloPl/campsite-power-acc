import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { fetchCurrentUser, login as loginCall, logout as logoutCall, type CurrentUser } from '../api/client';

interface AuthState {
  user: CurrentUser | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<CurrentUser>;
  logout: () => Promise<void>;
  hasRight: (right: string) => boolean;
}

const AuthContext = createContext<AuthState | null>(null);

/** Keeps the logged-in user; the backend holds the login in the HTTP session (cookie JSESSIONID). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    fetchCurrentUser()
      .then((current) => active && setUser(current))
      .catch(() => active && setUser(null))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const current = await loginCall(username, password);
    queryClient.clear();
    setUser(current);
    return current;
  }, [queryClient]);

  const logout = useCallback(async () => {
    try {
      await logoutCall();
    } finally {
      queryClient.clear();
      setUser(null);
    }
  }, [queryClient]);

  const hasRight = useCallback((right: string) => (user?.rights ?? []).includes(right), [user]);

  return <AuthContext.Provider value={{ user, loading, login, logout, hasRight }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const state = useContext(AuthContext);
  if (!state) throw new Error('useAuth outside AuthProvider');
  return state;
}
