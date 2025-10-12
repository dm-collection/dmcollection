import type { CardStub } from './types/card';
import type { CollectionInfo, CollectionData } from './types/collection';

export class Deck {
	collection: CollectionData | undefined = $state();
	id: string; // UUID v7

	getCards(): Array<CardStub> {
		return this.collection?.cardPage.content ?? [];
	}

	getInfo(): CollectionInfo | undefined {
		return this.collection?.info;
	}

	isLoaded(): boolean {
		return this.collection != undefined && this.collection != null;
	}

	constructor(id: string) {
		this.id = id;
	}

	async loadCollection(fetch: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>) {
			const response = await fetch(`/api/deck/${this.id}`);
			if (response.ok) {
				this.collection = (await response.json()) as CollectionData;
			} else if (response.status === 401 || response.status === 403) {
				throw new Error('unauthorized');
			} else {
				console.error(response.statusText);
			}
	}

	async getAmount(cardId: number): Promise<number> {
		return this.collection?.cardPage.content.find((c) => c.id == cardId)?.amount ?? 0;
	}

	async setCardAmount(cardId: number, amount: number) {
		if (this.collection) {
				const response = await fetch(`/api/deck/${this.collection.info.id}/cards/${cardId}`, {
					method: 'PUT',
					headers: {
						'Content-Type': 'application/json',
						'X-XSRF-TOKEN':
							document.cookie
								.split('; ')
								.find((row) => row.startsWith('XSRF-TOKEN='))
								?.split('=')[1] ?? ''
					},
					body: JSON.stringify({ amount: amount })
				});
				if (response.ok) {
					const updatedDeckInfo = (await response.json()) as CollectionInfo;
					if (!this.collection) {
						await this.loadCollection(fetch);
					} else {
						this.collection.info = updatedDeckInfo;
						const idx = this.collection.cardPage.content.findIndex((c) => c.id == cardId);
						if (idx >= 0) {
							if (amount == 0) {
								this.collection.cardPage.content.splice(idx, 1);
							} else {
								this.collection.cardPage.content[idx].amount = amount;
							}
						} else {
							await this.loadCollection(fetch);
						}
					}
				} else if (response.status === 401 || response.status === 403) {
					throw new Error('unauthorized');
				} else {
					console.log(response.statusText);
				}
		}
	}
}
