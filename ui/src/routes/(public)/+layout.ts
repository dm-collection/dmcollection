import type { LayoutLoad } from './$types';
import { checkAuthStatus } from '$lib/auth.svelte';
import { goto } from '$app/navigation';

export const load: LayoutLoad = async ({ fetch }) => {
	const authStatus = await checkAuthStatus(fetch);
	if (authStatus.authenticated) {
		await goto('/');
	}

	return { authStatus: authStatus };
};
