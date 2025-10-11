<script lang="ts">
	import { Pagination as PaginationPrimitive } from 'bits-ui';
	import { goto } from '$app/navigation';
	import type { Page } from '$lib/types/page';
	import { page } from '$app/state';
	import CaretRight from 'phosphor-svelte/lib/CaretRight';
	import CaretLeft from 'phosphor-svelte/lib/CaretLeft';

	let {
		pageInfo,
		path,
		onForward,
		onBack
	}: {
		pageInfo: Page;
		path?: string;
		onForward?: (newPageNum: number) => void;
		onBack?: (newPageNum: number) => void;
	} = $props();

	async function handlePageChange(newPage: number) {
		if (newPage === pageInfo.number) return;

		const isForward = newPage > pageInfo.number;

		// Callback mode - check if callbacks are provided
		if (isForward && onForward) {
			onForward(newPage);
		} else if (!isForward && onBack) {
			onBack(newPage);
		}
		// URL mode - navigate using path
		else if (path) {
			await goto(`${path}/${newPage}${page.url.search ? `${page.url.search.toString()}` : ''}`);
		}
	}
</script>

<PaginationPrimitive.Root
	count={pageInfo.totalElements}
	perPage={pageInfo.size}
	page={pageInfo.number}
	onPageChange={handlePageChange}
	class="my-4 flex flex-row items-center justify-center gap-2"
>
	{#snippet children({ pages, currentPage })}
		<PaginationPrimitive.PrevButton class="btn-primary" aria-label="previous page">
			<CaretLeft weight="bold" size="1.5em" />
		</PaginationPrimitive.PrevButton>

		{#each pages as pageItem (pageItem.key)}
			{#if pageItem.type === 'ellipsis'}
				<span class="px-2 text-base text-slate-500">...</span>
			{:else}
				<PaginationPrimitive.Page
					page={pageItem}
					class={pageItem.value === currentPage ? 'btn-primary' : 'btn-secondary'}
				>
					{pageItem.value}
				</PaginationPrimitive.Page>
			{/if}
		{/each}

		<PaginationPrimitive.NextButton class="btn-primary" aria-label="next page">
			<CaretRight weight="bold" size="1.5em" />
		</PaginationPrimitive.NextButton>
	{/snippet}
</PaginationPrimitive.Root>
