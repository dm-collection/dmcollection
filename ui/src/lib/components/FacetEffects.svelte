<script lang="ts">
	import type { CardEffect } from '$lib/types/card';
	import type { ClassValue } from 'svelte/elements';
	import EffectText from './EffectText.svelte';

	const props: { effects: Array<CardEffect>; class?: ClassValue } = $props();
</script>

<div>
	<ol class={['list-inside list-disc', props.class]}>
		{#each props.effects as effect (effect.position)}
			<li class="text-base">
				<EffectText text={effect.text}></EffectText>
				{#if effect.children}
					<ol class="list-inside pl-8">
						{#each effect.children as child (child.position)}
							<li class="text-base"><EffectText text={child.text}></EffectText></li>
						{/each}
					</ol>
				{/if}
			</li>
		{/each}
	</ol>
</div>

<style>
	ol > li > ol > li::marker {
		content: '▶ ';
		font-family: 'Segoe UI Symbol', 'Arial Unicode MS', sans-serif;
	}
	ol > li::marker {
		content: '■ ';
		font-family: 'Segoe UI Symbol', 'Arial Unicode MS', sans-serif;
	}
</style>
