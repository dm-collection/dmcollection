<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import Civilization from '$lib/components/card/Civilization.svelte';
	import { Civ } from '$lib/types/card';

	const { Story } = defineMeta({
		component: Civilization
	});
</script>

<Story name="colorless" args={{ civ: Civ.ZERO }} />
<Story name="light" args={{ civ: Civ.LIGHT }} />
<Story name="water" args={{ civ: Civ.WATER }} />
<Story name="dark" args={{ civ: Civ.DARK }} />
<Story name="fire" args={{ civ: Civ.FIRE }} />
<Story name="nature" args={{ civ: Civ.NATURE }} />
