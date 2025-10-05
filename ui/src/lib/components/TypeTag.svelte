<script lang="ts">
	import { CIV_COLORS, CIV_ORDER } from '$lib/civColors';
	import type { Civ } from '$lib/types/card';

	const { type, civs }: { type: string; civs: Civ[] } = $props();

	function makeStyle(civs: Civ[]) {
		let result = '';
		let present = 0;
		for (let i = 0; i < CIV_ORDER.length; i++) {
			if (civs.includes(CIV_ORDER[i])) {
				result = result + makeVar(CIV_ORDER[i], ++present);
			}
		}
		return result;
	}

	function makeVar(civ: Civ, index: number) {
		return `--c${index}: var(--color-${CIV_COLORS[civ]}); `;
	}
</script>

<h2
	class={[
		'mr-auto',
		'rounded-r-xl',
		'border-1',
		'border-black',
		'pr-2',
		'pl-4',
		'text-sm',
		'font-semibold',
		civs.length > 0 && `mana-${civs.length}`
	]}
	style={civs.length > 0 && civs.length < 5 ? makeStyle(civs) : ''}
>
	{type}
</h2>

<style>
	.mana-1 {
		background-color: var(--c1);
	}
	.mana-2 {
		background: linear-gradient(50deg, var(--c1) 0% 50%, var(--c2) 50% 100%);
	}
	.mana-3 {
		background: linear-gradient(50deg, var(--c1) 0% 33%, var(--c2) 33% 66%, var(--c3) 66% 100%);
	}
	.mana-4 {
		background: linear-gradient(
			50deg,
			var(--c1) 0% 25%,
			var(--c2) 25% 50%,
			var(--c3) 50% 75%,
			var(--c4) 75% 100%
		);
	}
	.mana-5 {
		background: linear-gradient(
			50deg,
			var(--color-light) 0% 20%,
			var(--color-water) 20% 40%,
			var(--color-dark) 40% 60%,
			var(--color-fire) 60% 80%,
			var(--color-nature) 80% 100%
		);
	}
</style>
