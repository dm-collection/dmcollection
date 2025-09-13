<script lang="ts">
	import type { CardFacet } from '$lib/types/card';
	import FacetEffects from './FacetEffects.svelte';
	import FacetHeader from './FacetHeader.svelte';

	let { facet }: { facet: CardFacet } = $props();
</script>

<div class="flex grow flex-col gap-4 rounded-md bg-white p-4 drop-shadow-md">
	<FacetHeader cost={facet.cost} civilizations={facet.civilizations} />
	{#if facet.name || facet.species}
		<div class="mb-8 flex flex-col gap-1">
			<h1 class="mx-auto text-xl font-bold">{facet.name}</h1>
			<h2 class="mx-auto text-sm font-light">
				<span
					>{#if facet.species}{#each facet.species as specie, i (specie)}{#if i > 0}/{/if}{specie}{/each}{/if}</span
				>
			</h2>
		</div>
	{/if}
	{#if facet.type}
		<h2 class="mr-auto rounded-r-xl border-1 border-black pr-2 pl-4 text-sm font-semibold">
			{facet.type}
		</h2>
	{/if}
	{#if facet.effects}
		<FacetEffects effects={facet.effects} class="ml-4" />
	{/if}
	{#if facet.flavor}
		<p class="text-base font-light">{facet.flavor}</p>
	{/if}
	{#if facet.power || facet.mana || facet.illustrator}
		<div class="mt-auto flex flex-row gap-1">
			{#if facet.power}
				<span
					class="p-y-1 inline-flex items-center rounded bg-neutral-100 px-2 text-xl font-bold text-neutral-700 ring-1 ring-neutral-600/10 ring-inset"
					>{facet.power}</span
				>
			{/if}
			{#if facet.mana}
				<span
					class="p-y-1 inline-flex items-center rounded bg-neutral-100 px-2 text-xs font-medium text-neutral-700 ring-1 ring-neutral-600/10 ring-inset"
					>{facet.mana}</span
				>
			{/if}
			{#if facet.illustrator}
				<span
					class="p-y-1 inline-flex items-center rounded bg-violet-200 px-2 text-xs font-medium text-violet-700 ring-1 ring-violet-600/10 ring-inset"
					>{facet.illustrator}
				</span>
			{/if}
		</div>
	{/if}
</div>
