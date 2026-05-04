<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import HistoryTable from '$lib/components/HistoryTable.svelte';

	const { Story } = defineMeta({
		component: HistoryTable
	});
</script>

<Story
	name="History Table"
	args={{
		class: ['md:w-2/3', 'lg:w-1/2'],
		entries: [
			[
				null,
				2,
				1,
				new Date().toISOString(),
				'dm24ex3-012',
				'DM24EX3 12/80',
				'ヨビニオン・クロウラー',
				356,
				'DM24-EX3 刺激爆発デュエナマイトパック',
				['dm24ex3-012.jpg']
			],
			[
				null,
				1,
				2,
				new Date(new Date().getDate() - 2).toISOString(),
				'dm24ex2-040',
				'DM24EX2 40/100',
				'運命の境界線',
				352,
				'DM24-EX2 天下夢双!!デュエキングDreaM 2024',
				['dm24ex2-040.jpg']
			],
			[
				null,
				2,
				3,
				'2026-05-02T23:12:24.708147+02:00',
				'spdeck13-001',
				'DMSPDECK13 1/11',
				'逆札神ヘヴィ・ウィン・メタル',
				385,
				'コロコロ・スペシャル デッキ13',
				['spdeck13-001a.jpg']
			],
			[
				'import',
				0,
				1,
				'2026-04-02T22:57:17.856474+02:00',
				'dm25ex1-063',
				'DM25EX1 63/89',
				'大長老 アプル／「わたしが自然文明のプリンセス？」',
				370,
				'DM25-EX1 愛感謝祭 ヒロインBEST',
				['dm25ex1-063a.jpg']
			],
			[
				'import',
				0,
				2,
				'2026-04-02T22:57:17.856474+02:00',
				'dmx23-023',
				'DMX23 23/60',
				'未来の玉 ダンテ',
				172,
				'DMX-23 革命ファイナル 奥義伝授!! デッキLv.マックスパック',
				['dmx23-023.jpg']
			],
			[
				'import',
				0,
				4,
				'2026-04-02T22:57:17.856474+02:00',
				'dm25ex1-058',
				'DM25EX1 58/89',
				'ハトメラル',
				370,
				'DM25-EX1 愛感謝祭 ヒロインBEST',
				['dm25ex1-058.jpg']
			]
		]
	}}
/>
