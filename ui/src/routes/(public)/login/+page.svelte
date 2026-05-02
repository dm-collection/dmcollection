<script lang="ts">
	import LoginForm from '$lib/components/LoginForm.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<LoginForm redirectPath={data.redirect} />
