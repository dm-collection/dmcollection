import { Civ } from './types/card';
import { CardTypeFilter } from './types/CardTypeFilter';
import { FilterState } from './types/FilterState';
import { Range } from './types/rarity';
import { Direction, SortingCriterion, type Order } from './types/sort';

const keys = {
	setId: 'setId',
	twinpact: 'twinpact',
	cardType: 'cardType',
	minCost: 'minCost',
	maxCost: 'maxCost',
	minPower: 'minPower',
	maxPower: 'maxPower',
	includeCivs: 'includeCivs',
	excludeCivs: 'excludeCivs',
	includeMono: 'includeMono',
	includeRainbow: 'includeRainbow',
	matchNumberOfCivs: 'matchNumberOfCivs',
	species: 'species',
	rarity: 'rarity',
	rarityRange: 'rRange',
	nameSearch: 'name',
	sort: 'sort'
};

export class SearchFilter {
	private readonly MAX_INT: number = 2147483647;
	private readonly MIN_INT: number = -2147483647;

	#setId: number | undefined = $state();

	#minCost: number | undefined = $state();
	#maxCost: number | undefined = $state();
	#minPower: number | undefined = $state();
	#maxPower: number | undefined = $state();

	#includedCivs: string[] = $state(Object.keys(Civ));
	#excludedCivs: string[] = $state([]);
	#includeMono: boolean = $state(true);
	#includeRainbow: boolean = $state(true);
	#rainbowsMustMatchAllIncludedCivs: boolean = $state(false);
	#twinpact: FilterState = $state(FilterState.IN);
	#cardType: keyof typeof CardTypeFilter | undefined = $state();
	#species: string | undefined = $state();
	#rarity: string | undefined = $state();
	#rarityRange: Range = $state(Range.EQ);
	#nameSearch: string | undefined = $state();
	#order: Order[] | undefined = $state();

	#searchParams: URLSearchParams = $state(new URLSearchParams());

	set setId(setId) {
		this.#searchParams.delete(keys.setId);
		if (setId != undefined) {
			this.#searchParams.set(keys.setId, setId.toString());
		}
		this.#setId = setId;
	}

	set rainbowsMustMatchAllIncludedCivs(rainbowsMustMatchAllIncludedCivs: boolean) {
		if (rainbowsMustMatchAllIncludedCivs) {
			this.#excludedCivs = Object.keys(Civ).filter(
				(civ) => !this.#includedCivs.includes(civ) && Civ.ZERO != Civ[civ as keyof typeof Civ]
			);
		} else {
			this.#excludedCivs = [];
		}
		this.#rainbowsMustMatchAllIncludedCivs = rainbowsMustMatchAllIncludedCivs;
		this.#changeCivFilter();
	}

	set includeMono(includeMono) {
		if (!includeMono && !this.#includeRainbow) {
			return;
		}
		this.#includeMono = includeMono;
		this.#changeCivFilter();
	}

	set includeRainbow(includeRainbow) {
		if (!this.#includeMono && !includeRainbow) {
			return;
		}
		this.#includeRainbow = includeRainbow;
		this.#changeCivFilter();
	}

	set includedCivs(includedCivs) {
		if (includedCivs.filter((civ) => civ != 'ZERO').length <= 1) {
			this.#rainbowsMustMatchAllIncludedCivs = false;
		}
		if (this.#rainbowsMustMatchAllIncludedCivs) {
			this.#excludedCivs = Object.keys(Civ).filter(
				(civ) => !includedCivs.includes(civ) && Civ.ZERO != Civ[civ as keyof typeof Civ]
			);
		} else {
			this.#excludedCivs = this.#excludedCivs.filter((civ) => !includedCivs.includes(civ));
		}
		this.#includedCivs = includedCivs;
		this.#changeCivFilter();
	}

	set excludedCivs(excludedCivs) {
		if (this.#includedCivs.length === 1) {
			return;
		}
		this.#includedCivs = this.#includedCivs.filter((civ) => !excludedCivs.includes(civ));
		this.#excludedCivs = excludedCivs;
		this.#changeCivFilter();
	}

	#changeCivFilter() {
		this.#searchParams.delete(keys.includeCivs);
		this.#searchParams.delete(keys.excludeCivs);
		this.#searchParams.delete(keys.includeMono);
		this.#searchParams.delete(keys.includeRainbow);
		this.#searchParams.delete(keys.matchNumberOfCivs);
		if (this.#includedCivs.length != Object.keys(Civ).length) {
			// only need included civs if not all are included
			this.#includedCivs.forEach((civ) => this.#searchParams.append(keys.includeCivs, civ));
		}
		if (this.#includeMono != this.#includeRainbow) {
			this.#searchParams.append(keys.includeMono, this.#includeMono ? 'true' : 'false');
			this.#searchParams.append(keys.includeRainbow, this.#includeRainbow ? 'true' : 'false');
		}
		if (this.#includeRainbow && this.#excludedCivs.length > 0) {
			this.#excludedCivs.forEach((civ) => this.#searchParams.append(keys.excludeCivs, civ));
		}
		if (this.#includeRainbow && this.#rainbowsMustMatchAllIncludedCivs == true) {
			this.#searchParams.append(keys.matchNumberOfCivs, 'true');
		}
	}

	#clampNumber(x: number | undefined): number | undefined {
		if (x == undefined) return undefined;
		if (x < this.MIN_INT) return this.MIN_INT;
		if (x > this.MAX_INT) return this.MAX_INT;
		return x;
	}

	set minCost(minCost) {
		minCost = this.#clampNumber(minCost);
		this.#searchParams.delete(keys.minCost);
		if (minCost !== undefined) {
			this.#searchParams.set(keys.minCost, minCost.toString());
		}
		this.#minCost = minCost;
	}

	set maxCost(maxCost) {
		maxCost = this.#clampNumber(maxCost);
		this.#searchParams.delete(keys.maxCost);
		if (maxCost !== undefined) {
			this.#searchParams.set(keys.maxCost, maxCost.toString());
		}
		this.#maxCost = maxCost;
	}

	set minPower(minPower) {
		minPower = this.#clampNumber(minPower);
		this.#searchParams.delete(keys.minPower);
		if (minPower != undefined) {
			this.#searchParams.set(keys.minPower, minPower.toString());
		}
		this.#minPower = minPower;
	}

	set maxPower(maxPower) {
		maxPower = this.#clampNumber(maxPower);
		this.#searchParams.delete(keys.maxPower);
		if (maxPower != undefined) {
			this.#searchParams.set(keys.maxPower, maxPower.toString());
		}
		this.#maxPower = maxPower;
	}

	set twinpact(twinpact) {
		this.#searchParams.delete(keys.twinpact);

		if (twinpact != FilterState.IN) {
			this.#searchParams.set(keys.twinpact, FilterState[twinpact]);
		}
		this.#twinpact = twinpact;
	}

	set cardType(cardType) {
		this.#searchParams.delete(keys.cardType);
		if (cardType != undefined) {
			this.#searchParams.set(keys.cardType, cardType);
		}
		this.#cardType = cardType;
	}

	set species(species) {
		this.#searchParams.delete(keys.species);
		if (species != undefined) {
			this.#searchParams.set(keys.species, species);
		}
		this.#species = species;
	}

	set rarity(rarity) {
		this.#searchParams.delete(keys.rarity);
		if (rarity != undefined) {
			this.#searchParams.set(keys.rarity, rarity);
		}
		this.#rarity = rarity;
	}

	set rarityRange(rarityRange) {
		this.#searchParams.delete(keys.rarityRange);
		if (rarityRange != Range.EQ) {
			this.#searchParams.set(keys.rarityRange, Range[rarityRange]);
		}
		this.#rarityRange = rarityRange;
	}

	set nameSearch(nameSearch) {
		this.#searchParams.delete(keys.nameSearch);
		if (nameSearch != undefined) {
			this.#searchParams.set(keys.nameSearch, nameSearch);
		}
		this.#nameSearch = nameSearch;
	}

	set order(order) {
		this.#searchParams.delete(keys.sort);
		if (order != undefined) {
			this.#searchParams.set(keys.sort, this.#stringifyOrder(order));
		}
		this.#order = order;
	}

	get setId() {
		return this.#setId;
	}

	get minCost() {
		return this.#minCost;
	}

	get maxCost() {
		return this.#maxCost;
	}

	get minPower() {
		return this.#minPower;
	}

	get maxPower() {
		return this.#maxPower;
	}

	get includedCivs() {
		return this.#includedCivs;
	}

	get excludedCivs() {
		return this.#excludedCivs;
	}

	get includeMono() {
		return this.#includeMono;
	}

	get includeRainbow() {
		return this.#includeRainbow;
	}

	get rainbowsMustMatchAllIncludedCivs() {
		return this.#rainbowsMustMatchAllIncludedCivs;
	}

	get twinpact() {
		return this.#twinpact;
	}

	get cardType() {
		return this.#cardType;
	}

	get species() {
		return this.#species;
	}

	get rarity() {
		return this.#rarity;
	}

	get rarityRange() {
		return this.#rarityRange;
	}

	get nameSearch() {
		return this.#nameSearch;
	}

	get order() {
		return this.#order;
	}

	get searchParams(): URLSearchParams {
		return this.#searchParams;
	}

	isDefault(): boolean {
		return (
			this.#setId == undefined &&
			this.#includedCivs.length == Object.keys(Civ).length &&
			this.#excludedCivs.length == 0 &&
			this.#includeMono &&
			this.#includeRainbow &&
			!this.#rainbowsMustMatchAllIncludedCivs &&
			this.#minCost == undefined &&
			this.#maxCost == undefined &&
			this.#minPower == undefined &&
			this.#maxPower == undefined &&
			this.#twinpact == FilterState.IN &&
			this.#cardType == undefined &&
			this.#species == undefined &&
			this.#rarity == undefined &&
			this.#rarityRange == Range.EQ &&
			this.#nameSearch == undefined &&
			this.#order == undefined
		);
	}

	restore(searchParams: URLSearchParams) {
		this.#searchParams = searchParams;
		const setIdParam = searchParams.get(keys.setId);
		this.#setId = setIdParam != null ? parseInt(setIdParam) : undefined;
		const includedCivParams = searchParams.getAll(keys.includeCivs);
		this.#includedCivs = includedCivParams.length > 0 ? includedCivParams : Object.keys(Civ);
		const excludedCivParams = searchParams.getAll(keys.excludeCivs);
		this.#excludedCivs = excludedCivParams.length > 0 ? excludedCivParams : [];
		this.#excludedCivs = this.#excludedCivs.filter((civ) => !this.#includedCivs.includes(civ));
		const incMonoParam = searchParams.get(keys.includeMono);
		this.#includeMono = incMonoParam !== 'false';
		this.#includeRainbow = searchParams.get(keys.includeRainbow) !== 'false';
		this.#rainbowsMustMatchAllIncludedCivs = searchParams.get(keys.matchNumberOfCivs) === 'true';

		const minCostParam = searchParams.get(keys.minCost);
		this.#minCost = minCostParam != null ? parseInt(minCostParam) : undefined;

		const maxCostParam = searchParams.get(keys.maxCost);
		this.#maxCost = maxCostParam != null ? parseInt(maxCostParam) : undefined;

		const minPowerParam = searchParams.get(keys.minPower);
		this.#minPower = minPowerParam != null ? parseInt(minPowerParam) : undefined;

		const maxPowerParam = searchParams.get(keys.maxPower);
		this.#maxPower = maxPowerParam != null ? parseInt(maxPowerParam) : undefined;

		const twinpactParam = searchParams.get(keys.twinpact);
		if (twinpactParam != null && Object.values(FilterState).includes(twinpactParam)) {
			this.#twinpact = FilterState[twinpactParam as keyof typeof FilterState];
		} else {
			this.#twinpact = FilterState.IN;
		}
		const cardTypeParam = searchParams.get(keys.cardType);
		if (cardTypeParam != null && Object.keys(CardTypeFilter).includes(cardTypeParam)) {
			this.#cardType = cardTypeParam as keyof typeof CardTypeFilter;
		}
		this.#species = searchParams.get(keys.species) ?? undefined;
		this.#rarity = searchParams.get(keys.rarity) ?? undefined;
		if (this.#rarity != undefined) {
			this.#rarityRange =
				Range[searchParams.get(keys.rarityRange) as keyof typeof Range] ?? Range.EQ;
		} else {
			this.#rarityRange = Range.EQ;
		}
		this.#nameSearch = searchParams.get(keys.nameSearch) ?? undefined;
		const orderParam = searchParams.get(keys.sort);
		if (orderParam != null) {
			this.#parseOrder(orderParam);
		}
	}

	#parseOrder(orderParam: string) {
		if (!orderParam.trim()) {
			this.#order = undefined;
			return;
		}
		this.#order = orderParam
			.split(',')
			.map((token) => token.trim())
			.filter((token) => token.length > 0)
			.map((token) => {
				const [property, direction] = token.split(':');
				if (!property || !direction) {
					return null;
				}
				const trimmedProperty = property.trim();
				const trimmedDirection = direction.trim().toUpperCase();

				if (!Object.values(SortingCriterion).includes(trimmedProperty as SortingCriterion)) {
					return null;
				}
				if (!Object.values(Direction).includes(trimmedDirection as Direction)) {
					return null;
				}
				return {
					property: trimmedProperty as SortingCriterion,
					direction: trimmedDirection as Direction
				};
			})
			.filter((order): order is Order => order !== null);
	}

	#stringifyOrder(orders: Order[]): string {
		return orders.map((order) => `${order.property}:${order.direction}`).join(',');
	}

	constructor(searchParams?: URLSearchParams) {
		if (searchParams != undefined) {
			this.#searchParams = searchParams;
			this.restore(searchParams);
		}
	}
}
