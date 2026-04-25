<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import SideHeader from '$lib/components/card/SideHeader.svelte';
	import { Civ } from '$lib/types/card';

	const { Story } = defineMeta({
		component: SideHeader,
		argTypes: {
			civilizations: {
				control: { type: 'multi-select' },
				options: [Civ.ZERO, Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE, Civ.NATURE]
			}
		},
		parameters: {
			layout: 'centered'
		}
	});
</script>

<Story name="No Cost" args={{ cost: null, civilizations: [Civ.LIGHT] }} />
<Story name="Light" args={{ cost: '3', civilizations: [Civ.LIGHT] }} />
<Story name="Water" args={{ cost: '5', civilizations: [Civ.WATER] }} />
<Story name="Dark" args={{ cost: '7', civilizations: [Civ.DARK] }} />
<Story name="Fire" args={{ cost: '4', civilizations: [Civ.FIRE] }} />
<Story name="Nature" args={{ cost: '6', civilizations: [Civ.NATURE] }} />
<Story name="Multi-Civ" args={{ cost: '8', civilizations: [Civ.LIGHT, Civ.WATER] }} />
<Story
	name="All Civs"
	args={{ cost: '∞', civilizations: [Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE, Civ.NATURE] }}
/>
