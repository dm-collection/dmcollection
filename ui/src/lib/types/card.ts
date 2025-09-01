export enum Civ {
	ZERO = 'ゼロ',
	LIGHT = '光',
	WATER = '水',
	DARK = '闇',
	FIRE = '火',
	NATURE = '自然'
}

export type Card = {
	id: number;
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
}

export type CardEffect = {
	text: string;
	position: number;
	children: Array<ChildEffect> | null;
}

export type CardFacet = {
	position: number | null;
	name: string;
	cost: string | null;
	civilizations: Array<Civ>;
	imagePath: string | null;
	flavor: string | null;
	type: string | null;
	species: Array<string> | null;
	effects: Array<CardEffect> | null;
	power: string | null;
	mana: string | null;
	illustrator: string | null;
};

export type CardStub = {
	id: number;
	dmId: string;
	idText: string;
	imagePaths: Array<string>;
	amount: number;
};
