<script lang="ts">
	import type { SearchFilter } from '$lib/SearchFilter.svelte';
	import { EFFECT_SEARCH_SUGGESTIONS } from '$lib/effectKeywords';
	import { Civ, type CardSet } from '$lib/types/card';
	import { CardTypeFilter } from '$lib/types/CardTypeFilter';
	import { FilterState } from '$lib/types/FilterState';
	import { Range, type Rarity } from '$lib/types/rarity';
	import {
		AMOUNT_ASC,
		AMOUNT_DESC,
		CARD_ID_ASC,
		CARD_ID_DESC,
		COST_ASC,
		COST_DESC,
		POWER_ASC,
		POWER_DESC,
		RARITY_ASC,
		RARITY_DESC,
		RELEASE_ASC,
		RELEASE_DESC,
		type Order
	} from '$lib/types/sort';
	import Civilization from './Civilization.svelte';
	import CaretDown from 'phosphor-svelte/lib/CaretDown';
	import CaretUp from 'phosphor-svelte/lib/CaretUp';
	import ArrowsLeftRight from 'phosphor-svelte/lib/ArrowsLeftRight';

	let {
		search,
		sets,
		species,
		rarities,
		changeCallback,
		showOwnedOnlyToggle = false,
		ownedOnly = $bindable(false)
	}: {
		search: SearchFilter;
		sets: Array<CardSet> | undefined;
		species: Array<string> | undefined;
		rarities: Array<Rarity> | undefined;
		changeCallback: (newParams: URLSearchParams, ownedOnly?: boolean) => void;
		showOwnedOnlyToggle?: boolean;
		ownedOnly?: boolean;
	} = $props();

	const MAX_ROUNDED: number = 2147490000;
	const MIN_ROUNDED: number = -MAX_ROUNDED;

	const eq: string = 'equal';
	const range: string = 'from';
	const betweenLabel: string = 'to';

	let filtersVisible: boolean = $state(true);
	let sortVisible: boolean = $state(false);

	type OrderOption = {
		order: Order;
		label: string;
	};

	const ORDER_OPTIONS = [
		{ order: RELEASE_DESC, label: 'Newest first' },
		{ order: RELEASE_ASC, label: 'Oldest first' },
		{ order: RARITY_DESC, label: 'Rarity (Descending)' },
		{ order: RARITY_ASC, label: 'Rarity (Ascending)' },
		{ order: COST_DESC, label: 'Cost (Descending)' },
		{ order: COST_ASC, label: 'Cost (Ascending)' },
		{ order: POWER_DESC, label: 'Power (Descending)' },
		{ order: POWER_ASC, label: 'Power (Ascending)' },
		{ order: AMOUNT_DESC, label: '# Owned (Descending)' },
		{ order: AMOUNT_ASC, label: '# Owned (Ascending' },
		{ order: CARD_ID_DESC, label: 'ID (Descending)' },
		{ order: CARD_ID_ASC, label: 'ID (Ascending)' }
	];

	let selectedSort1: OrderOption = $state(getOptionForOrderIndex(0, ORDER_OPTIONS[0]));
	let selectedSort2: OrderOption = $state(getOptionForOrderIndex(1, ORDER_OPTIONS[11]));

	let costSelectLabel: string | undefined = $state();
	let powerSelectLabel: string | undefined = $state();

	costSelectLabel =
		search.searchParams.has('costRange') && search.searchParams.get('costRange') !== 'false'
			? range
			: eq;

	powerSelectLabel =
		search.searchParams.has('powerRange') && search.searchParams.get('powerRange') !== 'false'
			? range
			: eq;

	function getOptionForOrderIndex(orderIndex: number, defaultOption: OrderOption): OrderOption {
		if (search.order != undefined && search.order.length > orderIndex) {
			const searchOrder = search.order[orderIndex];
			const candidate = ORDER_OPTIONS.find(
				(option) =>
					option.order.property == searchOrder.property &&
					option.order.direction == searchOrder.direction
			);
			if (candidate) {
				return candidate;
			}
		}
		return defaultOption;
	}

	async function changeSort() {
		if (selectedSort1.order == RELEASE_DESC && selectedSort2.order == CARD_ID_ASC) {
			search.order = undefined;
		} else {
			search.order = [selectedSort1.order, selectedSort2.order];
		}
		onChange();
	}

	async function switchSort() {
		[selectedSort1, selectedSort2] = [selectedSort2, selectedSort1];
		onChange();
	}

	async function changeMinCost() {
		if (costSelectLabel == eq) {
			search.maxCost = search.minCost;
		}
		onChange();
	}

	async function changeCostSelect() {
		if (costSelectLabel == eq) {
			search.searchParams.delete('costRange');
			search.maxCost = search.minCost;
			changeMinCost();
		} else {
			search.searchParams.append('costRange', 'true');
		}
	}

	async function changePowerSelect() {
		if (powerSelectLabel == eq) {
			search.searchParams.delete('powerRange');
			search.maxPower = search.minPower;
			changeMinPower();
		} else {
			search.searchParams.append('powerRange', 'true');
		}
	}

	async function changeMinPower() {
		if (powerSelectLabel == eq) {
			search.maxPower = search.minPower;
		}
		onChange();
	}

	async function onChange() {
		changeCallback(search.searchParams, ownedOnly);
	}
</script>

<div>
	<div class="mb-2 flex flex-row gap-2">
		<button
			class="btn-secondary"
			onclick={() => {
				filtersVisible = !filtersVisible;
				sortVisible = filtersVisible ? false : sortVisible;
			}}
		>
			Filter
			{#if filtersVisible}
				<CaretUp class="ml-2" weight="bold" size="1.5em"></CaretUp>
			{:else}
				<CaretDown class="ml-2" weight="bold" size="1.5em"></CaretDown>
			{/if}
		</button>
		<button
			class="btn-secondary"
			onclick={() => {
				sortVisible = !sortVisible;
				filtersVisible = sortVisible ? false : sortVisible;
			}}
		>
			Sort
			{#if sortVisible}
				<CaretUp class="ml-2" weight="bold" size="1.5em"></CaretUp>
			{:else}
				<CaretDown class="ml-2" weight="bold" size="1.5em"></CaretDown>
			{/if}
		</button>
	</div>
	<div class="flex flex-row flex-wrap items-start gap-4" class:hidden={!filtersVisible}>
		<div class:hidden={!showOwnedOnlyToggle} class="self-end">
			<label class="flex items-center gap-2">
				<input type="checkbox" bind:checked={ownedOnly} onchange={onChange} />
				<span class="text-sm font-medium">Owned only</span>
			</label>
		</div>
		<div>
			<label for="nameSearch" class="block text-sm font-medium">Card name</label>
			<input
				class="select"
				name="nameSearch"
				type="search"
				bind:value={search.nameSearch}
				onchange={onChange}
			/>
		</div>
		{#if sets != undefined}
			<div>
				<label for="sets" class="block text-sm font-medium">Set</label>
				<select
					class="select max-w-[33vw]"
					name="sets"
					bind:value={search.setId}
					onchange={onChange}
				>
					<option value={undefined}>All sets</option>
					{#each sets as set (set.id)}
						<option class="truncate" value={set.id}>{set.idText} - {set.name}</option>
					{/each}
				</select>
			</div>
		{/if}
		<div>
			<label for="rarity" class="block text-sm font-medium">レアリティ</label>
			<select
				class="select"
				name="rarityRange"
				bind:value={search.rarityRange}
				onchange={onChange}
				class:hidden={search.rarity == undefined}
			>
				<option value={Range.EQ}>=</option>
				<option value={Range.GE}>≥</option>
				<option value={Range.LE}>≤</option>
			</select>
			<select class="select" name="rarity" bind:value={search.rarity} onchange={onChange}>
				<option value={undefined}>指定なし</option>
				{#if rarities != undefined}
					{#each rarities as rarity (rarity.code)}
						<option value={rarity.code}>{rarity.code != 'NONE' ? rarity.code : rarity.name}</option>
					{/each}
				{/if}
			</select>
		</div>
		<div>
			<label for="twinpact" class="block text-sm font-medium">ツインパクト</label>
			<select class="select" name="twinpact" bind:value={search.twinpact} onchange={onChange}>
				<option value={FilterState.IN}>Included</option>
				<option value={FilterState.ONLY}>Only</option>
				<option value={FilterState.EX}>Excluded</option>
			</select>
		</div>
		<div>
			<label for="cardType" class="block text-sm font-medium">カードの種類</label>
			<select class="select" name="cardType" bind:value={search.cardType} onchange={onChange}>
				<option value={undefined}>指定なし</option>
				{#each Object.entries(CardTypeFilter) as [key, value] (key)}
					<option value={key}>{value}</option>
				{/each}
			</select>
		</div>
		<div>
			<label for="species" class="block text-sm font-medium">種族</label>
			<input
				id="species"
				type="search"
				class="select"
				list="speciesList"
				name="species"
				bind:value={search.species}
				onchange={onChange}
			/>
			{#if species != undefined}
				<datalist id="speciesList">
					{#each species as spec (spec)}
						<option value={spec}></option>
					{/each}
				</datalist>
			{/if}
		</div>
		<div>
			<label for="effectSearch" class="block text-sm font-medium">効果</label>
			<input
				id="effectSearch"
				type="search"
				class="select"
				list="effectKeywordList"
				name="effectSearch"
				bind:value={search.effectSearch}
				onchange={onChange}
			/>
			<datalist id="effectKeywordList">
				{#each EFFECT_SEARCH_SUGGESTIONS as keyword (keyword)}
					<option value={keyword}></option>
				{/each}
			</datalist>
		</div>
		<fieldset class="flex-col items-start gap-2">
			<legend class="text-sm font-medium">数</legend>
			<div class="flex-row gap-2">
				<label for="fromCost"
					>コスト<select class="select" bind:value={costSelectLabel} onchange={changeCostSelect}
						><option value={eq}>{eq}</option><option value={range}>{range}</option></select
					></label
				>
				<input
					class="w-20 rounded-md border border-gray-300 bg-white p-1 text-gray-700 sm:text-sm"
					id="fromCost"
					type="number"
					name="minCost"
					min={MIN_ROUNDED}
					max={MAX_ROUNDED}
					step="1"
					bind:value={search.minCost}
					onchange={changeMinCost}
				/>
				<label for="toCost" class:hidden={costSelectLabel == eq}>{betweenLabel}</label>
				<input
					class:hidden={costSelectLabel == eq}
					class="w-20 rounded-md border border-gray-300 bg-white p-1 text-gray-700 sm:text-sm"
					id="toCost"
					type="number"
					name="maxCost"
					min={MIN_ROUNDED}
					max={MAX_ROUNDED}
					step="1"
					bind:value={search.maxCost}
					onchange={onChange}
				/>
			</div>
			<div class="flex-row gap-2">
				<label for="fromPower"
					>パワー<select class="select" bind:value={powerSelectLabel} onchange={changePowerSelect}
						><option value={eq}>{eq}</option>
						<option value={range}>{range}</option>
					</select></label
				>
				<input
					class="w-20 rounded-md border border-gray-300 bg-white p-1 text-gray-700 sm:text-sm"
					id="fromPower"
					type="number"
					name="minPower"
					step="1000"
					min={MIN_ROUNDED}
					max={MAX_ROUNDED}
					bind:value={search.minPower}
					onchange={changeMinPower}
				/>
				<label for="toPower" class:hidden={powerSelectLabel == eq}>{betweenLabel}</label>
				<input
					class:hidden={powerSelectLabel == eq}
					class="w-20 rounded-md border border-gray-300 bg-white p-1 text-gray-700 sm:text-sm"
					id="toPower"
					type="number"
					name="maxPower"
					min={MIN_ROUNDED}
					max={MAX_ROUNDED}
					step="1000"
					bind:value={search.maxPower}
					onchange={onChange}
				/>
			</div>
		</fieldset>
		<fieldset>
			<legend class="text-sm font-medium">文明</legend>
			<fieldset class="flex flex-row gap-4">
				<label>
					<input type="checkbox" bind:checked={search.includeMono} onchange={onChange} />
					単色
				</label>
				<label>
					<input type="checkbox" bind:checked={search.includeRainbow} onchange={onChange} />
					多色
				</label>
				<label
					class:hidden={!search.includeRainbow ||
						search.includedCivs.filter((civ) => civ != 'ZERO').length <= 1}
				>
					<input
						type="checkbox"
						bind:checked={search.rainbowsMustMatchAllIncludedCivs}
						onchange={onChange}
					/>
					Rainbow cards must match all civilizations
				</label>
			</fieldset>
			<div class="flex flex-row gap-4">
				<fieldset class="mt-1.5 flex flex-row gap-4">
					<legend class="text-sm font-medium">Included:</legend>
					{#each Object.keys(Civ) as civ (civ)}
						<label class="text-sm font-medium">
							<input
								type="checkbox"
								value={civ}
								onchange={onChange}
								bind:group={search.includedCivs}
							/>
							<Civilization civ={Civ[civ as keyof typeof Civ]} />
						</label>
					{/each}
				</fieldset>
				<fieldset
					class="mt-1.5 flex flex-row gap-4"
					class:hidden={!search.includeRainbow ||
						search.rainbowsMustMatchAllIncludedCivs ||
						search.includedCivs.filter((civ) => civ != 'ZERO').length === 5 ||
						search.includedCivs.filter((civ) => civ != 'ZERO').length <= 1}
				>
					<legend class="text-sm font-medium">Excluded:</legend>
					{#each Object.keys(Civ).filter((civ) => Civ[civ as keyof typeof Civ] != Civ.ZERO && !search.includedCivs.includes(civ)) as civ (civ)}
						<label class="text-sm font-medium">
							<input
								type="checkbox"
								value={civ}
								onchange={onChange}
								bind:group={search.excludedCivs}
							/>
							<Civilization civ={Civ[civ as keyof typeof Civ]} />
						</label>
					{/each}
				</fieldset>
			</div>
		</fieldset>
	</div>
	<div class="flex flex-row flex-wrap items-start gap-4" class:hidden={!sortVisible}>
		<div>
			<label for="sort1" class="block text-sm font-medium">Sort by</label>
			<select class="select" name="sort1" onchange={changeSort} bind:value={selectedSort1}>
				{#each ORDER_OPTIONS as sortOption (sortOption.order)}
					<option
						value={sortOption}
						disabled={selectedSort2.order.property == sortOption.order.property}
						>{sortOption.label}</option
					>
				{/each}
			</select>
		</div>
		<button class="btn-secondary self-end" aria-label="Switch sorting order" onclick={switchSort}
			><ArrowsLeftRight weight="bold" size="1em"></ArrowsLeftRight></button
		>
		<div>
			<label for="sort2" class="block text-sm font-medium">Then sort by</label>
			<select class="select" name="sort2" onchange={changeSort} bind:value={selectedSort2}>
				{#each ORDER_OPTIONS as sortOption (sortOption.order)}
					<option
						value={sortOption}
						disabled={selectedSort1.order.property == sortOption.order.property}
						>{sortOption.label}</option
					>
				{/each}
			</select>
		</div>
	</div>
</div>
