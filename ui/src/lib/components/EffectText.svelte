<script lang="ts">
	import { Civ } from '$lib/types/card';
	import type { ClassValue } from 'svelte/elements';
	import CostIcon from './CostIcon.svelte';

	type TextSegment = {
		type: 'text';
		content: string;
	};

	type IconSegment = {
		type: 'icon';
		iconType: 'icon' | 'mana';
		placeholder: string;
		content: string;
		civs?: Civ[];
		cost?: string;
	};

	const symbolRegex = /\[\[(icon|mana):([^\]]*)\]\]/g;
	const manaRegex = /^([clwdnf]+)(\d+)$/;

	type ParsedSegment = TextSegment | IconSegment;

	const { text, class: className }: { text: string; class?: ClassValue } = $props();

	const segments = $derived(parseText(text));
	const startsWithIcon = $derived(segments.length > 0 && segments[0].type === 'icon');

	function parseText(input: string): ParsedSegment[] {
		const segments: ParsedSegment[] = [];
		let lastIndex = 0;
		let match;
		symbolRegex.lastIndex = 0;
		while ((match = symbolRegex.exec(input)) !== null) {
			if (match.index > lastIndex) {
				const textContent = input.slice(lastIndex, match.index);
				if (textContent.length > 0) {
					segments.push({
						type: 'text',
						content: textContent
					});
				}
			}

			const iconType = match[1] as 'icon' | 'mana';
			const iconContent = match[2];

			if (iconType === 'icon') {
				segments.push({
					type: 'icon',
					iconType: 'icon',
					placeholder: match[0],
					content: iconContent
				});
			} else if (iconType === 'mana') {
				const parsed = parseMana(iconContent);
				if (parsed) {
					segments.push(parsed);
				}
			}
			lastIndex = symbolRegex.lastIndex;
		}
		if (lastIndex < input.length) {
			const remainingText = input.slice(lastIndex);
			if (remainingText.length > 0) {
				segments.push({
					type: 'text',
					content: remainingText
				});
			}
		}
		return segments;
	}

	function parseMana(iconContent: string): IconSegment | undefined {
		const match = iconContent.match(manaRegex);

		if (!match) {
			throw new Error(`Invalid input format: "${iconContent}"`);
		}
		const [, civs, cost] = match;
		const civLetters = Array.from(new Set(civs.split('')));
		const parsedCivs: Civ[] = [];
		let parsedCost = '';
		civLetters.forEach((letter) => {
			switch (letter) {
				case 'c':
					parsedCivs.push(Civ.ZERO);
					break;
				case 'l':
					parsedCivs.push(Civ.LIGHT);
					break;
				case 'w':
					parsedCivs.push(Civ.WATER);
					break;
				case 'd':
					parsedCivs.push(Civ.DARK);
					break;
				case 'f':
					parsedCivs.push(Civ.FIRE);
					break;
				case 'n':
					parsedCivs.push(Civ.NATURE);
					break;
				default:
					throw new Error(`Invalid civ abbreviation "${letter}"`);
			}
		});
		for (const digit of cost) {
			if (digit !== '0') {
				parsedCost = parsedCost.concat(digit);
			}
		}
		if (parsedCost.length === 0) {
			parsedCost = '0';
		}
		return {
			type: 'icon',
			iconType: 'mana',
			placeholder: match[0],
			content: iconContent,
			civs: parsedCivs,
			cost: parsedCost
		};
	}
</script>

<span>
	{#each segments as segment, i (i)}
		{#if segment.type === 'text'}
			{segment.content}
		{:else if segment.type === 'icon'}
			{#if segment.iconType === 'mana' && segment.cost}
				<CostIcon civs={segment.civs ?? []} cost={segment.cost}></CostIcon>
			{:else if segment.iconType === 'icon'}
				<b>[{segment.content}]</b>
			{/if}
		{/if}
	{/each}
</span>
