export interface AuthStatus {
	authenticated: boolean;
	username: string | null;
	registrationCodeRequired: boolean;
}

export interface AuthState extends AuthStatus {
	isLoading: boolean;
}
