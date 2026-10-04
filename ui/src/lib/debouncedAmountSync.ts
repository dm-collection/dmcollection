type Entry = {
	confirmedAmount: number;
	targetAmount: number;
	timer?: ReturnType<typeof setTimeout>;
	inFlight?: Promise<void>;
};

/**
 * Debounce amount changes by ID
 */
export function createDebouncedAmountSync(
	send: (id: number, amount: number) => Promise<boolean>,
	onFailure: (id: number, confirmedAmount: number) => void,
	waitMs = 1000
) {
	const entries = new Map<number, Entry>();

	function sync(id: number) {
		const entry = entries.get(id);
		if (!entry) return;
		clearTimeout(entry.timer);
		entry.timer = undefined;

		const run = async () => {
			if (entry.targetAmount === entry.confirmedAmount) return;
			const amount = entry.targetAmount;
			let ok = false;
			try {
				ok = await send(id, amount);
			} catch (error) {
				console.error(error);
			}
			if (ok) {
				entry.confirmedAmount = amount;
			} else {
				entry.targetAmount = entry.confirmedAmount;
				onFailure(id, entry.confirmedAmount);
			}
		};
		// wait for an earlier request for the same ID so requests stay ordered
		const inFlight = (entry.inFlight ?? Promise.resolve()).then(run);
		entry.inFlight = inFlight;
		inFlight.then(() => {
			if (entry.inFlight === inFlight && entry.timer === undefined) {
				entries.delete(id);
			}
		});
	}

	return {
		set(id: number, previousAmount: number, newAmount: number) {
			let entry = entries.get(id);
			if (entry) {
				entry.targetAmount = newAmount;
			} else {
				entry = { confirmedAmount: previousAmount, targetAmount: newAmount };
				entries.set(id, entry);
			}
			clearTimeout(entry.timer);
			entry.timer = setTimeout(() => sync(id), waitMs);
		},
		flush() {
			for (const [id, entry] of entries) {
				if (entry.timer !== undefined) sync(id);
			}
		}
	};
}
