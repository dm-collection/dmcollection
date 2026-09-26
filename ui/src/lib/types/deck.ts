export type DeckPrintingStub = {
	id: number;
	officialId: string;
	idText: string;
	amount: number;
	collectionAmount: number;
	imageFileNames: Array<string>;
};

export type DeckCardStub = {
	id: number;
	name: string;
	zone: string;
	printings: Array<DeckPrintingStub>;
};

export type DeckInfo = {
	id: string; // UUID v7
	name: string;
	numberOfCards: number;
	numberOfCopies: number;
	lastModified: Date;
	ownerId: string; // UUID v7
};

export type DeckData = {
	info: DeckInfo;
	cards: Array<DeckCardStub>;
};
