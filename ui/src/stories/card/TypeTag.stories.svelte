<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import TypeTag from '$lib/components/card/TypeTag.svelte';
	import { Civ } from '$lib/types/card';

	const { Story } = defineMeta({
		component: TypeTag,
		parameters: {
			layout: 'centered'
		},
		argTypes: {
			civs: {
				control: { type: 'multi-select' },
				options: [Civ.ZERO, Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE, Civ.NATURE]
			},
			type: {
				control: { type: 'select' },
				options: [
					' ドラグハート・スーパー・クリーチャー',
					' サイキック・スーパー・クリーチャー',
					' ドラグハート・クリーチャー',
					' クリーチャー',
					' ルナティック・フィールド',
					' オレガ・オーラ',
					' 零龍の儀',
					' サイキック・クロスギア',
					' ヒストリック・フィールド',
					' デュエルメイト呪文',
					' WDフィールド',
					' DGフィールド',
					' ドリーム・クリーチャー',
					' 進化ドリーム・クリーチャー',
					' D2フィールド',
					' DMフィールド',
					' ドラグハート・タマシード',
					' 最終禁断クリーチャー',
					' NEOクリーチャー',
					' デュエルメイト・スーパー・クリーチャー',
					' ドラグハート・フォートレス',
					' G-NEOドリーム・クリーチャー',
					' ルール・プラス',
					' T2フィールド',
					' タマシード',
					' 進化クロスギア',
					' キング・クリーチャー',
					' デュエルメイト・クリーチャー',
					' G-NEOサイキック・クリーチャー',
					' 禁断クリーチャー',
					' 幸せのフィールド',
					' 零龍星雲',
					' クロスギア',
					' 城',
					' 進化サイキック・クリーチャー',
					' 呪文',
					' エグザイル・クリーチャー',
					' Mono Artifact',
					' ギガ・オレガ・オーラ',
					' NEO GRクリーチャー',
					' ドラグハート・ウエポン',
					' フェアリー・フィールド',
					' スター進化ドラグハート・スーパー・クリーチャー',
					' 零龍クリーチャー',
					' 進化エグザイル・クリーチャー',
					' サイキック・フィールド',
					' G城',
					' サイキック・クリーチャー',
					' スター進化クリーチャー',
					' S-MAX進化クリーチャー',
					' 禁断フィールド',
					' 最終禁断フィールド',
					' 禁断の鼓動',
					' 進化ドラグハート・クリーチャー',
					' NEOドリーム・クリーチャー',
					' ドラゴニック・フィールド',
					' 無月フィールド',
					' 土地',
					' 進化クリーチャー',
					' キング・セル',
					' G-NEOクリーチャー',
					' デュエリスト',
					' GRクリーチャー'
				]
			}
		}
	});
</script>

<Story name="No Civ" args={{ type: 'クリーチャー', civs: [] }} />
<Story name="Light" args={{ type: 'クリーチャー', civs: [Civ.LIGHT] }} />
<Story name="Water" args={{ type: 'クリーチャー', civs: [Civ.WATER] }} />
<Story name="Dark" args={{ type: 'クリーチャー', civs: [Civ.DARK] }} />
<Story name="Fire" args={{ type: 'クリーチャー', civs: [Civ.FIRE] }} />
<Story name="Nature" args={{ type: 'クリーチャー', civs: [Civ.NATURE] }} />
<Story name="Light/Water" args={{ type: 'クリーチャー', civs: [Civ.LIGHT, Civ.WATER] }} />
<Story
	name="Light/Water/Dark"
	args={{ type: 'クリーチャー', civs: [Civ.LIGHT, Civ.WATER, Civ.DARK] }}
/>
<Story
	name="Light/Water/Dark/Fire"
	args={{ type: 'クリーチャー', civs: [Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE] }}
/>
<Story
	name="All Five"
	args={{ type: 'クリーチャー', civs: [Civ.LIGHT, Civ.WATER, Civ.DARK, Civ.FIRE, Civ.NATURE] }}
/>
