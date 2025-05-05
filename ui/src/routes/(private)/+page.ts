import type { PageLoad } from './$types';
// Login page doesn't need protection
export const load: PageLoad = async () => {
	return {};
};
