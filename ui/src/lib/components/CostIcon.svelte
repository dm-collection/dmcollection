<script lang="ts">
	import { Civ } from '$lib/types/card';
	import { SvelteMap } from 'svelte/reactivity';

	const { civs, cost }: { civs: Civ[]; cost: string } = $props();

	const civKeys = new SvelteMap<Civ, string>();
	civKeys.set(Civ.ZERO, 'zero');
	civKeys.set(Civ.LIGHT, 'light');
	civKeys.set(Civ.WATER, 'water');
	civKeys.set(Civ.DARK, 'dark');
	civKeys.set(Civ.FIRE, 'fire');
	civKeys.set(Civ.NATURE, 'nature');

	function makeStyle(civs: Civ[]) {
		let result = '';
		const ordered = [Civ.ZERO, Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE, Civ.NATURE];
		let present = 0;
		for (let i = 0; i < ordered.length; i++) {
			if (civs.includes(ordered[i])) {
				result = result + makeVar(ordered[i], ++present);
			}
		}
		return result;
	}

	function makeVar(civ: Civ, index: number) {
		return `--c${index}: var(--color-${civKeys.get(civ)}); `;
	}
</script>

<span
	class={[
		'manacircle',
		'font-extrabold',
		'rounded-full',
		'items-center',
		'justify-center',
		`mana${civs.length}`
	]}
	style={makeStyle(civs)}>{cost}</span
>

<style>
	.manacircle {
		aspect-ratio: 1 / 1;
		color: #ffffff;
		display: inline-flex;
		text-shadow:
			black 0 0 2px,
			black 1px 1px 3px;
		box-shadow: black 0 0 2px;
		border: solid black 1px;
		min-width: 1.5em;
		min-height: 1.5em;
	}
	.mana0 {
		background-color: var(--color-slate-200);
	}
	.mana1 {
		background-color: var(--c1);
	}
	.mana2 {
		background: conic-gradient(from 135deg, var(--c1) 0deg 180deg, var(--c2) 180deg 360deg);
	}
	.mana3 {
		background: conic-gradient(
			from 240deg,
			var(--c1) 0deg 120deg,
			var(--c2) 120deg 240deg,
			var(--c3) 240deg 360deg
		);
	}
	.mana4 {
		background: conic-gradient(
			from 225deg,
			var(--c1) 0deg 90deg,
			var(--c2) 90deg 180deg,
			var(--c3) 180deg 270deg,
			var(--c4) 270deg 360deg
		);
	}
	.mana5 {
		background: conic-gradient(
			from 288deg,
			var(--color-light) 0deg 72deg,
			var(--color-water) 72deg 144deg,
			var(--color-dark) 144deg 216deg,
			var(--color-fire) 216deg 288deg,
			var(--color-nature) 288deg 360deg
		);
	}
</style>
