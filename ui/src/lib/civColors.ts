import { Civ } from '$lib/types/card';
export const CIV_COLORS = {
	[Civ.ZERO]: 'zero',
	[Civ.LIGHT]: 'light',
	[Civ.WATER]: 'water',
	[Civ.DARK]: 'dark',
	[Civ.FIRE]: 'fire',
	[Civ.NATURE]: 'nature'
} as const;

export const CIV_ORDER = [Civ.ZERO, Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE, Civ.NATURE] as const;
