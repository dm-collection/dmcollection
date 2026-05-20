import { loadLatest } from '$lib/api/history';
import type { PageLoad } from './$types';
export const load: PageLoad = async ({ fetch }) => {
	return {
		latest: await loadLatest(fetch, 5)
	};
};
