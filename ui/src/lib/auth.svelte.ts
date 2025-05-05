import { goto, invalidate } from '$app/navigation';
import type { AuthState, AuthStatus } from './types/auth';

export const authState: AuthState = $state({
	authenticated: false,
	username: null,
	registrationCodeRequired: false,
	isLoading: false
});

export async function checkAuthStatus(
	fetch: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>
): Promise<AuthState> {
	try {
		const response = await fetch('/api/auth/status');
		const data: AuthStatus = await response.json();
		authState.authenticated = data.authenticated;
		authState.username = data.username;
		authState.registrationCodeRequired = data.registrationCodeRequired;
		authState.isLoading = false;
		return authState;
	} catch {
		authState.authenticated = false;
		authState.username = null;
		authState.isLoading = false;
		return authState;
	}
}

export async function invalidateAuth() {
	authState.authenticated = false;
	authState.username = null;
	authState.isLoading = false;
	invalidate((url) => url.pathname.startsWith('/api/auth/'));
}

interface LoginCredentials {
	username: string;
	password: string;
}

interface RegistrationRequest extends LoginCredentials {
	code: string | null;
}

export async function login(
	fetch: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>,
	credentials: LoginCredentials
): Promise<AuthState> {
	try {
		const response = await fetch('/api/auth/login', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-XSRF-TOKEN':
					document.cookie
						.split('; ')
						.find((row) => row.startsWith('XSRF-TOKEN='))
						?.split('=')[1] ?? ''
			},

			body: JSON.stringify(credentials)
		});
		if (response.ok) {
			const status = (await response.json()) as AuthStatus;
			authState.authenticated = status.authenticated;
			authState.username = status.username;
			authState.isLoading = false;
			return authState;
		} else {
			console.error(
				`Authentication failed with status ${response.status} and body ${await response.text()}`
			);
			return checkAuthStatus(fetch);
		}
	} catch {
		return checkAuthStatus(fetch);
	}
}

export async function checkUsername(
	fetch: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>,
	username: string
): Promise<boolean> {
	try {
		const response = await fetch('/api/auth/checkUsername', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-XSRF-TOKEN':
					document.cookie
						.split('; ')
						.find((row) => row.startsWith('XSRF-TOKEN='))
						?.split('=')[1] ?? ''
			},

			body: JSON.stringify({ username: username })
		});
		if (response.ok) {
			return (await response.json()) as boolean;
		} else {
			return false;
		}
	} catch {
		return false;
	}
}

export async function register(
	fetch: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>,
	credentials: RegistrationRequest
): Promise<AuthState> {
	try {
		const response = await fetch('/api/auth/register', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-XSRF-TOKEN':
					document.cookie
						.split('; ')
						.find((row) => row.startsWith('XSRF-TOKEN='))
						?.split('=')[1] ?? ''
			},

			body: JSON.stringify(credentials)
		});
		if (response.ok) {
			const status = (await response.json()) as AuthStatus;
			authState.authenticated = status.authenticated;
			authState.username = status.username;
			authState.isLoading = false;
			return authState;
		} else {
			console.error(
				`Registration failed with status ${response.status} and body ${await response.text()}`
			);
			return checkAuthStatus(fetch);
		}
	} catch {
		return checkAuthStatus(fetch);
	}
}

export async function logout(
	fetch: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>
): Promise<void> {
	try {
		const response = await fetch('/api/auth/logout', {
			method: 'POST',
			headers: {
				'X-XSRF-TOKEN':
					document.cookie
						.split('; ')
						.find((row) => row.startsWith('XSRF-TOKEN='))
						?.split('=')[1] ?? ''
			}
		});

		if (response.ok) {
			invalidateAuth();
			// Redirect to login page
			await goto('/');
		} else {
			console.error('Logout failed');
		}
	} catch (error) {
		console.error('Logout error:', error);
	}
}
