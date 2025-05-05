export type PagedResult<T> = {
	content: Array<T>;
	page: Page;
};

export type Page = {
	size: number;
	number: number;
	totalElements: number;
	totalPages: number;
};
