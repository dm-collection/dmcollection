<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import NavigationMenu from '$lib/components/NavigationMenu.svelte';
	let { children } = $props();
</script>

<svelte:head>
	<title>DM Collection</title>
</svelte:head>

<header>
	<NavigationMenu enabled={true} username={auth.user?.username} />
</header>
<main class="sticky m-8 mb-2">
	{@render children()}
</main>
