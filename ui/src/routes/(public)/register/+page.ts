import { loadRegistrationCodeRequired } from '$lib/api/register';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
	const response = await loadRegistrationCodeRequired(fetch);
	if (response.ok) {
		return { registrationCodeRequired: (await response.json()) as boolean };
	} else {
		return { registrationCodeRequired: false };
	}
};
