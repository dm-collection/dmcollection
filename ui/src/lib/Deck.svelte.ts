import type { DeckCardStub, DeckData, DeckInfo } from './types/deck';

export class Deck {
	deck: DeckData | undefined = $state();
	id: string; // UUID v7

	getCards(): Array<DeckCardStub> {
		return this.deck?.cards ?? [];
	}

	getInfo(): DeckInfo | undefined {
		return this.deck?.info;
	}

	isLoaded(): boolean {
		return this.deck != undefined && this.deck != null;
	}

	constructor(id: string) {
		this.id = id;
	}

	async loadDeck(fetch: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>) {
		const response = await fetch(`/api/deck/${this.id}`);
		if (response.ok) {
			this.deck = (await response.json()) as DeckData;
		} else if (response.status === 401 || response.status === 403) {
			throw new Error('unauthorized');
		} else {
			console.error(response.statusText);
		}
	}

	async getAmount(printingId: number): Promise<number> {
		return (
			this.deck?.cards.flatMap((c) => c.printings).find((p) => p.id == printingId)?.amount ?? 0
		);
	}

	async setPrintingAmount(printingId: number, amount: number) {
		if (this.deck) {
			const response = await fetch(`/api/deck/${this.deck.info.id}/cards/${printingId}`, {
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
				const updatedDeckInfo = (await response.json()) as DeckInfo;
				if (!this.deck) {
					await this.loadDeck(fetch);
				} else {
					this.deck.info = updatedDeckInfo;

					let found = false;
					for (let i = 0; i < this.deck.cards.length; i++) {
						const idx = this.deck.cards[i].printings.findIndex((p) => p.id == printingId);
						if (idx >= 0) {
							found = true;
							if (amount == 0) {
								this.deck.cards[i].printings.splice(idx, 1);
							} else {
								this.deck.cards[i].printings[idx].amount = amount;
							}
							break;
						}
					}

					if (!found) {
						await this.loadDeck(fetch);
					}
				}
			} else if (response.status === 401 || response.status === 403) {
				throw new Error('unauthorized');
			} else {
				console.error(response.statusText);
			}
		}
	}
}
