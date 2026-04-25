<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import SideDetails from '$lib/components/card/SideDetails.svelte';

	const { Story } = defineMeta({
		component: SideDetails
	});
</script>

<Story
	name="Bolshack Dragon"
	args={{
		facet: {
			position: 0,
			name: 'ボルシャック・ドラゴン',
			cost: '6',
			civilizations: ['火'],
			power: '6000+',
			type: 'クリーチャー',
			species: ['アーマード・ドラゴン'],
			illustrator: 'Ryoya Yuki',
			flavor: 'その怒りに触れたために、ひとつの都市が消滅した。',
			mana: '1',
			effects: [
				{
					text: 'W・ブレイカー(シールドを攻撃したとき、このクリーチャーはシールドを２枚ブレイクする。)',
					position: 0,
					children: []
				},
				{
					text: '攻撃中、このクリーチャーのパワーは、自分の墓地にある火のカード１枚につき+1000される。',
					position: 1,
					children: []
				}
			]
		}
	}}
/>

<Story
	name="Legend Horn"
	args={{
		facet: {
			position: 0,
			name: '熱血剣 レジェンドホーン',
			cost: '4',
			civilizations: ['水', '火', '自然'],
			power: null,
			type: 'ドラグハート・ウエポン',
			species: [],
			illustrator: 'SENNSU',
			effects: [
				{
					text: 'これを装備したクリーチャーが水なら「ブロッカー」を、火なら「スピードアタッカー」を、自然なら「マッハファイター」を与える。',
					position: 0,
					children: []
				},
				{
					text: 'このドラグハートが出た時、自分の山札の上から１枚を見る。そのカードを手札に加えるか、タップしてマナゾーンに置く。',
					position: 1,
					children: []
				},
				{
					text: '龍解：自分のクリーチャーが攻撃する時、それがそのターン最初のクリーチャーによる攻撃でなければ、その攻撃の後、このドラグハートをクリーチャー側に裏返す。',
					position: 2,
					children: []
				},
				{
					text: '（ゲーム開始時、ドラグハートは自身の超次元ゾーンに置き、ドラグハートまたはそれを装備したクリーチャーが離れたら、そこに戻す）',
					position: 3,
					children: []
				}
			]
		}
	}}
/>
