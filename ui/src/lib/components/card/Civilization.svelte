<script lang="ts">
	import { Civ } from '$lib/types/card';
	let { civ }: { civ: Civ } = $props();
	const bgColorMap: Record<Civ, string> = {
		[Civ.ZERO]: 'bg-slate-50',
		[Civ.LIGHT]: 'bg-yellow-100',
		[Civ.WATER]: 'bg-blue-50',
		[Civ.DARK]: 'bg-gray-500',
		[Civ.FIRE]: 'bg-red-50',
		[Civ.NATURE]: 'bg-green-50'
	};

	const textColorMap: Record<Civ, string> = {
		[Civ.ZERO]: 'text-slate-800',
		[Civ.LIGHT]: 'text-yellow-800',
		[Civ.WATER]: 'text-blue-700',
		[Civ.DARK]: 'text-white',
		[Civ.FIRE]: 'text-red-700',
		[Civ.NATURE]: 'text-green-700'
	};

	const ringColorMap: Record<Civ, string> = {
		[Civ.ZERO]: 'ring-slate-600/10',
		[Civ.LIGHT]: 'ring-yellow-600/20',
		[Civ.WATER]: 'ring-blue-700/10',
		[Civ.DARK]: 'ring-gray-800/10',
		[Civ.FIRE]: 'ring-red-600/10',
		[Civ.NATURE]: 'ring-green-600/10'
	};
	let bgColor = $derived(bgColorMap[civ]);
	let textColor = $derived(textColorMap[civ]);
	let ringColor = $derived(ringColorMap[civ]);
</script>

<span
	class="inline-flex items-center justify-center rounded-md {bgColor} {textColor} {ringColor} h-8 w-8 text-xs font-medium ring-1 ring-inset"
	>{civ}</span
>
