export type FetchFn = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;

export class ApiError extends Error {
	constructor(
		public readonly status: number,
		public readonly statusText: string,
		public readonly body: string
	) {
		super(`API ${status} ${statusText}`);
		this.name = 'ApiError';
	}
}

export interface ApiOptions extends Omit<RequestInit, 'body' | 'method'> {
	method?: string;
	json?: unknown;
	binary?: BodyInit;
	parseJson?: boolean;
}

export function getCsrfToken(): string {
	return (
		document.cookie
			.split('; ')
			.find((row) => row.startsWith('XSRF-TOKEN='))
			?.split('=')[1] ?? ''
	);
}

export async function apiFetch<T = unknown>(
	path: string,
	options: ApiOptions = {},
	fetchFn: FetchFn = globalThis.fetch
): Promise<T> {
	const { json, binary, parseJson, headers, method, ...rest } = options;
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

	if (!response.ok) {
		const errorBody = await response.text().catch(() => '');
		throw new ApiError(response.status, response.statusText, errorBody);
	}

	const shouldParse = parseJson ?? !(httpMethod === 'DELETE' || response.status === 204);
	if (!shouldParse) {
		return undefined as T;
	}
	return (await response.json()) as T;
}
