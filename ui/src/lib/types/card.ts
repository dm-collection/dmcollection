export enum Civ {
	ZERO = 'ゼロ',
	LIGHT = '光',
	WATER = '水',
	DARK = '闇',
	FIRE = '火',
	NATURE = '自然'
}

export type CardStub = {
	id: number;
	name: string;
	printings: Array<PrintingStub>;
};

export type PrintingStub = {
	id: number;
	officialId: string;
	idText: string;
	setCode: string;
	setRelease: string;
	amount: number;
	imageFileNames: Array<string>;
};

export type Printing = {
	id: number;
	name: string;
	dmId: string;
	idText: string;
	set: CardSet;
	civilizations: Array<Civ>;
	facets: Array<CardFacet>;
	rarity: string | null;
	amount: number | null;
};

export type CardSet = {
	id: number;
	idText: string;
	name: string;
};

export type ChildEffect = {
	text: string;
	position: number;
};

export type CardEffect = {
	text: string;
	position: number;
	children: Array<ChildEffect> | null;
};

export type CardFacet = {
	position: number | null;
	name: string;
	cost: string | null;
	civilizations: Array<Civ>;
	imageFile: string | null;
	flavor: string | null;
	type: string | null;
	species: Array<string> | null;
	effects: Array<CardEffect> | null;
	power: string | null;
	mana: string | null;
	illustrator: string | null;
};

export type OldPrintingStub = {
	id: number;
	dmId: string;
	idText: string;
	imageFiles: Array<string>;
	amount: number;
	collectionAmount: number;
};
