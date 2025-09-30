import type { LayoutLoad } from './$types';
import { resolve } from '$app/paths';
import { checkAuthStatus } from '$lib/auth.svelte';
import { goto } from '$app/navigation';

export const load: LayoutLoad = async ({ fetch, url }) => {
	const authStatus = await checkAuthStatus(fetch);

	if (!authStatus.authenticated) {
		sessionStorage.setItem('redirectAfterLogin', url.pathname);
		await goto(resolve('/login'));
	}
	return { authStatus: authStatus };
};
