import type { LayoutLoad } from './$types';
import { auth } from '$lib/auth.svelte';
import { redirect } from '@sveltejs/kit';

export const load: LayoutLoad = async ({ url }) => {
	await auth.refresh();
	if (auth.isAuthenticated) {
		const redirectTo = decodeURIComponent(url.searchParams.get('redirect') ?? '/');
		throw redirect(302, redirectTo);
	}
};
