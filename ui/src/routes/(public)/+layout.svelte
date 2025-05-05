<script lang="ts">
	import HeaderNav from '$lib/components/HeaderNav.svelte';
	let { children } = $props();
</script>

<HeaderNav enabled={false} />
<main class="mx-auto max-w-7xl px-4 py-6">
	{@render children()}
</main>
