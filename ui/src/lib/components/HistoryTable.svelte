<script lang="ts">
	import type { HistoryEntry } from '$lib/types/history';
	import { formatDistanceToNow } from 'date-fns';
	import type { ClassValue } from 'svelte/elements';

	const props: { entries: HistoryEntry[]; class: ClassValue } = $props();
</script>

<div
	class={[
		'border-teal-700',
		'border-4',
		'rounded-2xl',
		'bg-white',
		'flex',
		'flex-col',
		props.class
	]}
>
	<h2 class="border-b-2 border-teal-700 text-center text-lg font-bold">Recent Changes</h2>
	<div
		class="mx-4 grid grid-cols-[60px_max-content_auto] grid-rows-[minmax(0,1fr)] items-center gap-x-4 gap-y-4 p-1"
	>
		{#each props.entries as [label, oldQuantity, newQuantity, modified, dmId, collectionNumber, cardName, setId, setName, images] (`${dmId}|${modified}`)}
			<div class="image-cell">
				<a href={`/card/${dmId}`}>
					{#if images[0]}
						<img
							src={`/image/${images[0]}`}
							alt={`Card image of ${collectionNumber} from ${setName}`}
							title={`${collectionNumber}`}
							class="h-full w-full cursor-pointer object-contain"
						/>
					{/if}
				</a>
			</div>
			<div
				class="flex flex-col items-center"
				title={formatDistanceToNow(modified, { addSuffix: true })}
			>
				{#if label == null}
					<p class="font-bold">
						{`${newQuantity < oldQuantity ? '-' : '+'}${Math.abs(newQuantity - oldQuantity)}`}
					</p>
				{:else}
					<p class="font-bold">+{newQuantity}</p>
					<p class="text-xs font-light">[import]</p>
				{/if}
			</div>
			<div class="flex flex-col">
				<p>
					<a
						class="cursor-pointer text-sm text-teal-700 underline visited:text-violet-700 visited:decoration-dashed"
						href={`/cards?name=${cardName}`}>{cardName}</a
					>
				</p>
				<p class="text-xs font-light">
					(<a
						class="cursor-pointer text-teal-700 underline visited:text-violet-700 visited:decoration-dashed"
						href={`/cards?setId=${setId}`}
						>{setName.startsWith('DM') ? setName.split(' ')[0] : setName}</a
					>)
				</p>
			</div>
		{/each}
	</div>
</div>

<style>
	.image-cell {
		display: flex;
		min-width: 0;
		min-height: 0;
		width: 100%;
		height: 100%;
		align-content: center;
		justify-content: center;
		contain: size paint;
	}
</style>
