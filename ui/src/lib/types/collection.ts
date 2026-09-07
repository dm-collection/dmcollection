import type {CardStub} from './card';
import type {PagedResult} from './page';

export type CollectionInfo = {
	id: string; // UUID v7
	name: string;
	numberOfCopies: number;
	numberOfPrintings: number;
	numberOfCards: number;
	lastModified: Date;
	ownerId: string; // UUID v7
};

export type CollectionCard = {
	card: CardStub;
	amount: number;
};

export type CollectionData = {
	info: CollectionInfo;
	cardPage: PagedResult<CardStub>;
};
