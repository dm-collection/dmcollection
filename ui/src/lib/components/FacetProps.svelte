<script lang="ts">
	import type { CardFacet } from '$lib/types/card';
	import FacetEffects from './FacetEffects.svelte';
	import FacetHeader from './FacetHeader.svelte';

	let { facet }: { facet: CardFacet } = $props();
</script>

<div class="flex grow flex-col gap-8 rounded-md bg-white p-4 drop-shadow-md">
	<FacetHeader cost={facet.cost} civilizations={facet.civilizations} />
	{#if facet.name || facet.type || facet.species}
		<div class="flex flex-col gap-1">
			<h2 class="text-sm font-light">
				<span
					>{#if facet.type}{facet.type}{/if}</span
				>
				<span
					>{#if facet.type && facet.species && facet.species.length > 0}
						|
					{/if}</span
				>
				<span
					>{#if facet.species}{#each facet.species as specie, i (specie)}{#if i > 0}/{/if}{specie}{/each}{/if}</span
				>
			</h2>
			<h1 class="text-lg font-semibold">{facet.name}</h1>
		</div>
	{/if}
	{#if facet.effects }
	<FacetEffects effects={facet.effects}/>
	{/if}
	{#if facet.flavor}
	<p class="text-base font-light">{facet.flavor}</p>
	{/if}
	{#if facet.power || facet.mana || facet.illustrator}
		<div class="flex flex-row gap-1">
			{#if facet.power}
				<span
					class="p-y-1 inline-flex items-center rounded bg-neutral-100 px-2 text-xs font-medium text-neutral-700 ring-1 ring-neutral-600/10 ring-inset"
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
