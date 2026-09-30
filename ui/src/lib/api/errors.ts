import { error } from '@sveltejs/kit';
import { goto } from '$app/navigation';
import { ApiError } from './client';

export function handleApiError(err: unknown): boolean {
	if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
		goto('/login');
		return true;
	}
	return false;
}

export function loadGuard(err: unknown): never {
	if (err instanceof ApiError) {
		if (err.status === 401 || err.status === 403) {
			error(err.status, 'unauthorized');
		}
		error(err.status, err.statusText || 'Request failed');
	}
	throw err;
}
