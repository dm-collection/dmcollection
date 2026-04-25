<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import LoginForm from '$lib/components/LoginForm.svelte';

	const { Story } = defineMeta({
		component: LoginForm
	});
</script>

<Story name="Login form" />
