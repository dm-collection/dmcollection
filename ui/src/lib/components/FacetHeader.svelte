<script lang="ts">
	import Civilization from './Civilization.svelte';
	import { Civ } from '$lib/types/card';

	let { cost, civilizations }: { cost: string | null; civilizations: Array<Civ> } = $props();
	let hasCost = $state(cost != undefined && cost != null);
</script>

<div class="flex flex-row" class:justify-end={!hasCost} class:justify-between={hasCost}>
	{#if cost}
		<div
			class="inline-flex h-8 w-8 items-center justify-center overflow-hidden rounded-full bg-slate-200"
		>
			<span class="text-lg font-semibold">{cost}</span>
		</div>
	{/if}
	<div class=" flex flex-row flex-wrap gap-1">
		{#each civilizations as civ}
			{#if civ != undefined}
				<Civilization {civ} />
			{/if}
		{/each}
	</div>
</div>
