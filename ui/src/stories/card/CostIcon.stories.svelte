<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import CostIcon from '$lib/components/card/CostIcon.svelte';
	import { Civ } from '$lib/types/card';

	const { Story } = defineMeta({
		component: CostIcon,
		argTypes: {
			civs: {
				control: { type: 'multi-select' },
				options: [Civ.ZERO, Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE, Civ.NATURE]
			}
		},
		parameters: {
			layout: 'centered'
		}
	});
</script>

<Story name="Colorless" args={{ civs: [Civ.ZERO], cost: 0 }} />
<Story name="Light" args={{ civs: [Civ.LIGHT], cost: 1 }} />
<Story name="Water" args={{ civs: [Civ.WATER], cost: 2 }} />
<Story name="Dark" args={{ civs: [Civ.DARK], cost: 3 }} />
<Story name="Fire" args={{ civs: [Civ.FIRE], cost: 4 }} />
<Story name="Nature" args={{ civs: [Civ.NATURE], cost: 5 }} />
<Story name="Light/Water" args={{ civs: [Civ.LIGHT, Civ.WATER], cost: 6 }} />
<Story name="Dark/Fire/Nature" args={{ civs: [Civ.DARK, Civ.FIRE, Civ.NATURE], cost: 7 }} />
<Story
	name="Light/Water/Dark/Fire"
	args={{ civs: [Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE], cost: 99 }}
/>
<Story
	name="All Infinity"
	args={{ civs: [Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE, Civ.NATURE], cost: '∞' }}
/>
