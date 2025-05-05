import type { LayoutLoad } from './$types';
import { checkAuthStatus } from '$lib/auth.svelte';

export const load: LayoutLoad = async ({ fetch }) => {
	const authStatus = await checkAuthStatus(fetch);
	return { authStatus: authStatus };
};
