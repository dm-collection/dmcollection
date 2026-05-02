import { goto, invalidateAll } from '$app/navigation';
import { api } from './api';

export interface LoginCredentials {
	username: string;
	password: string;
}

export interface UserData {
	username: string;
}

interface LoginRequest extends LoginCredentials {
	rememberMe: boolean;
}

class AuthStore {
	user = $state<UserData | null>(null);

	get isAuthenticated() {
		return this.user !== null;
	}

	async refresh() {
		try {
			const res = await fetch('/api/auth/me');
			this.user = res.ok ? await res.json() : null;
		} catch {
			this.user = null;
		}
	}

	async login(loginRequest: LoginRequest, redirectPath?: string) {
		const res = await api('/api/auth/login', { json: loginRequest });
		if (res.ok) {
			this.user = await res.json();
			if (redirectPath) {
				await goto(redirectPath);
			}
		}
	}

	async logout() {
		await api('/api/auth/logout', { method: 'POST' });
		this.user = null;
		invalidateAll();
		await goto('/login');
	}
}

export const auth = new AuthStore();
