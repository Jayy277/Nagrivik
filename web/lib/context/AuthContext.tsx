'use client';

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { AuthUser, AuthStatus, AuthState } from '../../types/auth';

interface AuthContextType extends AuthState {
  loginWithIdToken: (idToken: string) => Promise<{ success: boolean; error?: string }>;
  logout: () => Promise<void>;
  checkSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AuthState>({
    status: 'LOADING',
    user: null,
    isLoading: true,
  });

  const checkSession = useCallback(async () => {
    try {
      const res = await fetch('/api/auth/me', { cache: 'no-store' });
      if (res.ok) {
        const data = await res.json();
        if (data && data.user) {
          setState({
            status: 'AUTHENTICATED',
            user: data.user,
            isLoading: false,
          });
          return;
        }
      }
      setState({
        status: 'UNAUTHENTICATED',
        user: null,
        isLoading: false,
      });
    } catch {
      setState({
        status: 'UNAUTHENTICATED',
        user: null,
        isLoading: false,
      });
    }
  }, []);

  useEffect(() => {
    checkSession();
  }, [checkSession]);

  const loginWithIdToken = async (idToken: string): Promise<{ success: boolean; error?: string }> => {
    setState((prev) => ({ ...prev, isLoading: true }));
    try {
      const res = await fetch('/api/auth/session', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken }),
      });

      if (res.ok) {
        const data = await res.json();
        setState({
          status: 'AUTHENTICATED',
          user: data.user,
          isLoading: false,
        });
        return { success: true };
      }

      const errData = await res.json().catch(() => ({}));
      setState((prev) => ({ ...prev, isLoading: false }));
      return {
        success: false,
        error: errData.message || 'Unable to sign in with Google. Please try again.',
      };
    } catch {
      setState((prev) => ({ ...prev, isLoading: false }));
      return {
        success: false,
        error: "Google sign-in succeeded, but Nagrivic couldn't complete your login. Please try again.",
      };
    }
  };

  const logout = async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } finally {
      setState({
        status: 'UNAUTHENTICATED',
        user: null,
        isLoading: false,
      });
    }
  };

  return (
    <AuthContext.Provider
      value={{
        ...state,
        loginWithIdToken,
        logout,
        checkSession,
      }}>
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
