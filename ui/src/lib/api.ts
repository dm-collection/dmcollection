import { auth } from './auth.svelte';
import { goto } from '$app/navigation';

export type FetchFn = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;

export interface ApiOptions extends Omit<RequestInit, 'body' | 'method'> {
	method?: string;
	json?: unknown;
	binary?: BodyInit;
	fetchFn?: FetchFn;
}

function getCsrfToken(): string {
	return (
		document.cookie
			.split('; ')
			.find((row) => row.startsWith('XSRF-TOKEN='))
			?.split('=')[1] ?? ''
	);
}

export async function api(path: string, options: ApiOptions = {}): Promise<Response> {
	const { json, binary, headers, method, fetchFn = fetch, ...rest } = options;
	const httpMethod = (
		method ?? (json !== undefined || binary !== undefined ? 'POST' : 'GET')
	).toUpperCase();
	const finalHeaders = new Headers(headers);
	let body: BodyInit | undefined;
	if (json !== undefined) {
		finalHeaders.set('Content-Type', 'application/json');
		body = JSON.stringify(json);
	} else if (binary !== undefined) {
		finalHeaders.set('Content-Type', 'application/octet-stream');
		body = binary;
	}
	if (httpMethod !== 'GET' && httpMethod !== 'HEAD') {
		finalHeaders.set('X-XSRF-TOKEN', getCsrfToken());
	}

	const response = await fetchFn(path, {
		...rest,
		method: httpMethod,
		headers: finalHeaders,
		body
	});

	if (response.status === 401) {
		auth.user = null;
		console.log(`not authenticated to get ${path} via ${location.pathname}`);
		if (!location.pathname.startsWith('/login')) {
			const redirect = encodeURIComponent(location.pathname + location.search);
			goto(`/login?redirect=${redirect}`);
		}
	}
	return response;
}
