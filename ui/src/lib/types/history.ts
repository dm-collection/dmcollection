export type HistoryEntry = [
	label: string | null,
	oldQuantity: number,
	newQuantity: number,
	modified: string,
	dmId: string,
	collectionNumber: string,
	cardName: string,
	setId: number,
	setName: string,
	images: string[]
];
