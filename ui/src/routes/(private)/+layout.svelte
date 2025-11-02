<script lang="ts">
	import { page } from '$app/state';
	import { authState } from '$lib/auth.svelte';
	import NavigationMenu from '$lib/components/NavigationMenu.svelte';
	let { children } = $props();
	let noScroll = $derived(page.url.pathname.includes('/deck/'));
</script>

<svelte:head>
	<title>DM Collection</title>
</svelte:head>

<div class={['flex', !noScroll && 'h-screen', 'flex-col', noScroll && 'overflow-hidden']}>
	<header class="sticky top-0 z-20 min-h-0 shrink-0">
		<NavigationMenu enabled={true} username={authState.username} />
	</header>
	<main class={['flex-1 p-3 md:p-8', noScroll ? 'overflow-hidden' : 'overflow-y-auto ']}>
		{@render children()}
	</main>
</div>
