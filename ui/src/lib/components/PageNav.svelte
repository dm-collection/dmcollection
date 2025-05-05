<script lang="ts">
	import { goto } from '$app/navigation';
	import type { Page } from '$lib/types/page';
	import { page } from '$app/stores';
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

	async function goBack() {
		if (onBack != undefined) {
			onBack(pageInfo.number - 1);
		} else {
			await goto(
				`${path}/${pageInfo.number - 1}${$page.url.search ? `${$page.url.search.toString()}` : ''}`
			);
		}
	}

	async function goForward() {
		if (onForward != undefined) {
			onForward(pageInfo.number + 1);
		} else {
			await goto(
				`${path}/${pageInfo.number + 1}${$page.url.search ? `${$page.url.search.toString()}` : ''}`
			);
		}
	}
</script>

<div class="my-4 flex flex-row justify-between">
	<button
		aria-label="previous page"
		class="btn-primary"
		onclick={goBack}
		disabled={pageInfo.number <= 1}
	>
		<CaretLeft weight="bold" size="1.5em"></CaretLeft>
	</button>
	<p class="text-base font-semibold">Page {pageInfo.number}/{pageInfo.totalPages}</p>
	<button
		aria-label="next page"
		class="btn-primary"
		onclick={goForward}
		disabled={pageInfo.number >= pageInfo.totalPages}
	>
		<CaretRight weight="bold" size="1.5em"></CaretRight>
	</button>
</div>
