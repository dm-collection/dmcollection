<script lang="ts">
	import HistoryTable from '$lib/components/HistoryTable.svelte';
	import type { PageProps } from './$types';

	const { data }: PageProps = $props();
</script>

<div class="flex flex-col items-center">
<HistoryTable entries={data.latest} class="md:max-w-2/3" />
</div>
