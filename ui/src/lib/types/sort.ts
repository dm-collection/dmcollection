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

export const RELEASE_ASC: Order = {
	property: SortingCriterion.Release,
	direction: Direction.Ascending
};

export const RELEASE_DESC: Order = {
	property: SortingCriterion.Release,
	direction: Direction.Descending
};

export const CARD_ID_ASC: Order = {
	property: SortingCriterion.CardId,
	direction: Direction.Ascending
};

export const CARD_ID_DESC: Order = {
	property: SortingCriterion.CardId,
	direction: Direction.Descending
};

export const AMOUNT_ASC: Order = {
	property: SortingCriterion.Amount,
	direction: Direction.Ascending
};

export const AMOUNT_DESC: Order = {
	property: SortingCriterion.Amount,
	direction: Direction.Descending
};

export const COST_ASC: Order = {
	property: SortingCriterion.Cost,
	direction: Direction.Ascending
};

export const COST_DESC: Order = {
	property: SortingCriterion.Cost,
	direction: Direction.Descending
};

export const POWER_ASC: Order = {
	property: SortingCriterion.Power,
	direction: Direction.Ascending
};

export const POWER_DESC: Order = {
	property: SortingCriterion.Power,
	direction: Direction.Descending
};

export const RARITY_ASC: Order = {
	property: SortingCriterion.Rarity,
	direction: Direction.Ascending
};

export const RARITY_DESC: Order = {
	property: SortingCriterion.Rarity,
	direction: Direction.Descending
};
