<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import { fn } from 'storybook/test';
	import Pagination from '$lib/components/Pagination.svelte';

	const { Story } = defineMeta({
		component: Pagination,
		parameters: { layout: 'centered' },
		args: {
			onForward: fn(),
			onBack: fn()
		}
	});
</script>

<Story
	name="Middle"
	args={{ pageInfo: { size: 10, number: 5, totalElements: 200, totalPages: 20 } }}
/>

<Story
	name="First Page"
	args={{ pageInfo: { size: 10, number: 1, totalElements: 200, totalPages: 20 } }}
/>

<Story
	name="Last Page"
	args={{ pageInfo: { size: 10, number: 20, totalElements: 200, totalPages: 20 } }}
/>

<Story
	name="Few Pages"
	args={{ pageInfo: { size: 10, number: 2, totalElements: 30, totalPages: 3 } }}
/>

<Story
	name="Single Page"
	args={{ pageInfo: { size: 10, number: 1, totalElements: 5, totalPages: 1 } }}
/>
