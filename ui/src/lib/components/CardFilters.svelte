<script lang="ts">
	import type { SearchFilter } from '$lib/SearchFilter.svelte';
	import { Civ, type CardSet } from '$lib/types/card';
	import { CardTypeFilter } from '$lib/types/CardTypeFilter';
	import { FilterState } from '$lib/types/FilterState';
	import { Range, type Rarity } from '$lib/types/rarity';
	import Civilization from './Civilization.svelte';

	const {
		search,
		sets,
		species,
		rarities,
		changeCallback
	}: {
		search: SearchFilter;
		sets: Array<CardSet> | undefined;
		species: Array<string> | undefined;
		rarities: Array<Rarity> | undefined;
		changeCallback: (newParams: URLSearchParams) => void;
	} = $props();

	const MAX_ROUNDED: number = 2147490000;
	const MIN_ROUNDED: number = -MAX_ROUNDED;

	const eq: string = 'equal';
	const range: string = 'from';
	const betweenLabel: string = 'to';

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
		changeCallback(search.searchParams);
	}
</script>

<div class="flex flex-row flex-wrap items-start gap-4">
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
			<select class="select max-w-[33vw]" name="sets" bind:value={search.setId} onchange={onChange}>
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
