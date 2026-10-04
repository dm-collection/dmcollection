<script lang="ts">
	import type { PrintingSide } from '$lib/types/card';
	import AbilityText from './AbilityText.svelte';
	import SideHeader from './SideHeader.svelte';
	import TypeTag from './TypeTag.svelte';

	let { side }: { side: PrintingSide } = $props();
</script>

<div class="flex grow flex-col gap-4 rounded-md bg-white p-4 drop-shadow-md">
	<SideHeader cost={side.cost} civilizations={side.civilizations} />
	{#if side.name || side.species}
		<div class="mb-8 flex flex-col gap-1">
			<h1 class="mx-auto text-xl font-bold">{side.name}</h1>
			<h2 class="mx-auto text-sm font-light">
				<span
					>{#if side.species}{#each side.species as specie, i (specie)}{#if i > 0}/{/if}{specie}{/each}{/if}</span
				>
			</h2>
		</div>
	{/if}
	{#if side.type}
		<TypeTag type={side.type} civs={side.civilizations} />
	{/if}
	{#if side.effects}
		<AbilityText effects={side.effects} class="ml-4" />
	{/if}
	{#if side.flavor}
		<p class="text-base font-light">{side.flavor}</p>
	{/if}
	{#if side.power || side.mana || side.illustrator}
		<div class="mt-auto flex flex-row gap-1">
			{#if side.power}
				<span
					class="p-y-1 inline-flex items-center rounded bg-neutral-100 px-2 text-xl font-bold text-neutral-700 ring-1 ring-neutral-600/10 ring-inset"
					>{side.power}</span
				>
			{/if}
			{#if side.mana}
				<span
					class="p-y-1 inline-flex items-center rounded bg-neutral-100 px-2 text-xs font-medium text-neutral-700 ring-1 ring-neutral-600/10 ring-inset"
					>{side.mana}</span
				>
			{/if}
			{#if side.illustrator}
				<span
					class="p-y-1 inline-flex items-center rounded bg-violet-200 px-2 text-xs font-medium text-violet-700 ring-1 ring-violet-600/10 ring-inset"
					>{side.illustrator}
				</span>
			{/if}
		</div>
	{/if}
</div>
