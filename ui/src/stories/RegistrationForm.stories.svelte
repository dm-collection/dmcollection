<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import RegistrationForm from '$lib/components/RegistrationForm.svelte';

	const { Story } = defineMeta({
		component: RegistrationForm
	});
</script>

<Story name="No registration code" />
<Story name="Code required" args={{ codeRequired: true }} />
