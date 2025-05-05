import type { ParamMatcher } from '@sveltejs/kit';

export const match = ((param: string) => {
	const num = parseFloat(param);
	return Number.isInteger(num) && num >= 1;
}) satisfies ParamMatcher;
