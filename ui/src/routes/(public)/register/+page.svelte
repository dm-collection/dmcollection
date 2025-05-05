<script lang="ts">
	import RegistrationForm from '$lib/components/RegistrationForm.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<RegistrationForm codeRequired={data.authStatus.registrationCodeRequired} />
