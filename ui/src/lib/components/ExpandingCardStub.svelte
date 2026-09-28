<script lang="ts">
	import type { CardStub } from '$lib/types/card';
	import type { DeckCardStub } from '$lib/types/deck';

	const {
		card,
		amount,
		max,
		sizes = '100vw',
		onClick
	}: {
		card: CardStub | DeckCardStub;
		amount: number;
		max?: number;
		sizes?: string;
		onClick: () => void;
	} = $props();
	const imageStack = $derived(
		card.printings
			.flatMap((p) => (p.imageFileNames.length > 0 ? p.imageFileNames[0] : []))
			.slice(0, 7)
	);
</script>

<div
	class="group flex flex-col items-center rounded-t-lg rounded-b-md border border-gray-300 bg-white pb-2 hover:drop-shadow-md active:bg-teal-50"
>
	<div class="flex flex-col items-center">
		{#if imageStack.length > 0}
			<button class="relative" onclick={onClick} aria-label="expand">
				{#each imageStack.toReversed() as imageName, i (imageName)}
					<img
						src={`/image/${imageName}`}
						srcset={`/image/250x0/${imageName} 250w, /image/650x0/${imageName} 650w`}
						{sizes}
						alt={card.name}
						class={[
							'rounded-md',
							'object-cover',
							i == imageStack.length && 'group-hover:opacity-90',
							i === 0 ? 'relative' : 'absolute inset-0 h-full w-full'
						]}
						style={i > 0
							? `z-index: ${i}; transform: scale(${1 - i * 0.01}); transform-origin: top left;`
							: ''}
					/>
				{/each}
			</button>
		{/if}
		<p class="inline-flex items-center">{card.printings.length} printings</p>
	</div>
	<div class="flex grow flex-col items-center justify-center">
		<p
			class="inline-flex h-7.5 w-7.5 items-center justify-center rounded-md bg-slate-50 text-lg font-medium ring-1 ring-slate-300 ring-inset md:h-8.5 md:w-8.5"
		>
			{amount}{max != undefined ? `/${max}` : ''}
		</p>
	</div>
</div>
