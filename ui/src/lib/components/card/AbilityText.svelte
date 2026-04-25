<script lang="ts">
	import type { CardEffect } from '$lib/types/card';
	import type { ClassValue } from 'svelte/elements';
	import AbilityTextLine from './AbilityTextLine.svelte';

	const props: { effects: Array<CardEffect>; class?: ClassValue } = $props();
</script>

<div>
	<ol class={['list-inside', props.class]}>
		{#each props.effects as effect (effect.position)}
			<li class={['text-base', !effect.text.startsWith('[[') && 'noIcon']}>
				<AbilityTextLine text={effect.text}></AbilityTextLine>
				{#if effect.children && effect.children.length > 0}
					<ol class="list-inside pl-8">
						{#each effect.children as child (child.position)}
							<li class={['text-base', !child.text.startsWith('[[') && 'choice']}>
								<AbilityTextLine text={child.text}></AbilityTextLine>
							</li>
						{/each}
					</ol>
				{/if}
			</li>
		{/each}
	</ol>
</div>

<style>
	.choice,
	.noIcon {
		list-style-type: none;
	}

	.choice::before {
		content: '▶ ';
		font-family: 'Segoe UI Symbol', 'Arial Unicode MS', sans-serif;
	}

	.noIcon::before {
		content: '■ ';
		font-family: 'Segoe UI Symbol', 'Arial Unicode MS', sans-serif;
	}
</style>
