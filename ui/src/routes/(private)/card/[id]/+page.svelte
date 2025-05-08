<script lang="ts">
	import type { PageData } from './$types';
	import { CardDetails } from '$lib/components';

	let { data }: { data: PageData } = $props();
</script>

<svelte:head>
	<title>{data.card?.idText ?? 'DM Collection'}</title>
</svelte:head>

{#if data.card != undefined}
	<CardDetails card={data.card} collectionEntry={data.collectionEntry} />
{:else}
	<h1>NOT FOUND</h1>
{/if}
