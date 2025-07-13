export enum Direction {
	Ascending = 'asc',
	Descending = 'desc'
}

export enum SortingCriterion {
	Release = 'rel',
	CardId = 'id',
	Amount = 'amt',
	Cost = 'cost',
	Power = 'pwr',
	Rarity = 'rar'
}

export type Order = {
	property: SortingCriterion;
	direction: Direction;
};
