<script lang="ts">
	import NavigationMenu from '$lib/components/NavigationMenu.svelte';
	let { children } = $props();
</script>

<svelte:head>
	<title>DM Collection</title>
</svelte:head>

<header>
	<NavigationMenu enabled={false} />
</header>
<main class="mx-auto max-w-7xl px-4 py-6">
	{@render children()}
</main>
