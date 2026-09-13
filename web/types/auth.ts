export interface AuthUser {
  id: string;
  phoneNumber?: string | null;
  email?: string | null;
  role: string;
  fullName: string | null;
  profilePictureUrl?: string | null;
}

export type AuthStatus = 'AUTHENTICATED' | 'UNAUTHENTICATED' | 'LOADING';

export interface AuthState {
  status: AuthStatus;
  user: AuthUser | null;
  isLoading: boolean;
}
