import type { CardSet } from '$lib/types/card';
import type { Rarity } from '$lib/types/rarity';
import { apiFetch } from './client';

export const fetchRarities = () => apiFetch<Rarity[]>('/api/rarities');
export const fetchSets = () => apiFetch<CardSet[]>('/api/sets');
export const fetchSpecies = () => apiFetch<string[]>('/api/species');
