import type { LayoutLoad } from './$types';
import { auth } from '$lib/auth.svelte';
import { redirect } from '@sveltejs/kit';

export const load: LayoutLoad = async ({ url }) => {
	await auth.refresh();
	if (!auth.isAuthenticated) {
		const redirectParam =
			url.pathname == '/' ? '' : `?redirect=${encodeURIComponent(url.pathname + url.search)}`;
		const redirectTo = `/login${redirectParam}`;
		throw redirect(302, redirectTo);
	}
};
