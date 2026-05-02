import { api, type FetchFn } from '../api';
import type { LoginCredentials, UserData } from '$lib/auth.svelte';
import { auth } from '$lib/auth.svelte';

export const loadRegistrationCodeRequired = (fetch: FetchFn) =>
	api('/api/auth/register', { fetchFn: fetch });

export const isUserNameAvailable = (username: string) => {
	return api(`/api/auth/available?username=${username}`).then((response) => {
		if (response.ok) {
			return response.json() as Promise<boolean>;
		} else {
			return Promise.reject(response.statusText);
		}
	});
};

interface RegistrationRequest extends LoginCredentials {
	code: string | null;
}

export const register = async (userData: RegistrationRequest) => {
	const response = await api('/api/auth/register', { json: userData });
	if (response.ok) {
		auth.refresh();
		return response.json() as Promise<UserData>;
	} else {
		return Promise.reject(response.statusText);
	}
};
