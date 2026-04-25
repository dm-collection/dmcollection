<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import AmountButton from '$lib/components/AmountButton.svelte';
	import { fn } from 'storybook/test';

	const { Story } = defineMeta({
		component: AmountButton,
		args: {
			max: 4,
			min: 0,
			onChange: fn()
		}
	});
</script>

<Story name="min" args={{ value: 0 }} />
<Story name="limit shown" args={{ value: 3, showMax: true, enforceMax: true }} />
<Story name="over limit" args={{ value: 6, showMax: true, enforceMax: false }} />
