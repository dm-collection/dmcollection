<script lang="ts">
	import type { PageData } from './$types';
	import { CardDetails } from '$lib/components';

	let { data }: { data: PageData } = $props();
</script>

<svelte:head>
	<title>{data.printing?.idText ?? 'DM Collection'}</title>
</svelte:head>

{#if data.printing != undefined}
	<CardDetails printing={data.printing} collectionEntry={data.collectionEntry} />
{:else}
	<h1>NOT FOUND</h1>
{/if}
