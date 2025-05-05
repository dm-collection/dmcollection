import type { LayoutLoad } from './$types';
import { checkAuthStatus } from '$lib/auth.svelte';
import { goto } from '$app/navigation';

export const load: LayoutLoad = async ({ fetch, url }) => {
	const authStatus = await checkAuthStatus(fetch);

	if (!authStatus.authenticated) {
		sessionStorage.setItem('redirectAfterLogin', url.pathname);
		await goto('/login');
	}
	return { authStatus: authStatus };
};
