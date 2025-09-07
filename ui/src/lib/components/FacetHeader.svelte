<script lang="ts">
	import Civilization from './Civilization.svelte';
	import { Civ } from '$lib/types/card';
	import CostIcon from './CostIcon.svelte';

	let { cost, civilizations }: { cost: string | null; civilizations: Array<Civ> } = $props();
	let hasCost = $state(cost != undefined && cost != null);
</script>

<div class="flex flex-row text-xl" class:justify-end={!hasCost} class:justify-between={hasCost}>
	{#if cost}
		<CostIcon civs={civilizations} {cost}></CostIcon>
	{/if}
	<div class=" flex flex-row flex-wrap gap-1">
		{#each civilizations as civ (civ)}
			{#if civ != undefined}
				<Civilization {civ} />
			{/if}
		{/each}
	</div>
</div>
