<script lang="ts">
	import { authState } from '$lib/auth.svelte';
	import HeaderNav from '$lib/components/HeaderNav.svelte';
	let { children } = $props();
</script>

<svelte:head>
	<title>DM Collection</title>
</svelte:head>

<HeaderNav enabled={true} username={authState.username ?? undefined} />
<main class="sticky m-8 mb-2">
	{@render children()}
</main>
