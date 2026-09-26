import type { CardStub } from './card';
import type { PagedResult } from './page';

export type CollectionInfo = {
	id: string; // UUID v7
	numberOfCopies: number;
	numberOfCards: number;
	ownerId: string; // UUID v7
};

export type CollectionData = {
	info: CollectionInfo;
	cardPage: PagedResult<CardStub>;
};
