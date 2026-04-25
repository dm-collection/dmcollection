<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import NavigationMenu from '$lib/components/NavigationMenu.svelte';

	const { Story } = defineMeta({
		component: NavigationMenu,
		parameters: {
			layout: 'fullscreen'
		}
	});
</script>

<Story name="Logged In" args={{ username: 'User' }} />
<Story name="Logged Out" args={{ enabled: false }} />
