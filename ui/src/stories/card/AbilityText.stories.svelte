<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import AbilityText from '$lib/components/card/AbilityText.svelte';

	const examples = {
		icons: [
			{
				text: 'D・D・D[[mana:lfn04]]（自分のクリーチャーが攻撃する時、このカードを[[mana:lfn04]]支払って自分の手札から実行してもよい）',
				position: 0,
				children: []
			},
			{
				text: 'G-NEO進化：光、火、または自然のクリーチャー１体の上に置いてもよい。（カードが下にあれば、NEO進化クリーチャーとして扱い、離れる時、かわりに下のカードすべてが離れる）',
				position: 1,
				children: []
			},
			{
				text: '[[icon:blocker]]ブロッカー',
				position: 2,
				children: []
			},
			{
				text: 'スピードアタッカー',
				position: 3,
				children: []
			},
			{
				text: 'マッハファイター',
				position: 4,
				children: []
			},
			{
				text: 'W・ブレイカー',
				position: 5,
				children: []
			},
			{
				text: '自分のクリーチャーがバトルに勝った時、自分の山札の上から１枚を見て、手札に加えるかマナゾーンに置く。その後、自分のマナゾーンにあるカードをすべてアンタップする。',
				position: 6,
				children: []
			}
		],
		choice: [
			{
				text: '[[icon:strigger]]S・トリガー（このカードを自分のシールドゾーンから手札に加える時、コストを支払わずにすぐ実行してもよい）',
				position: 0,
				children: []
			},
			{
				text: '次の中から１回選ぶ。',
				position: 1,
				children: [
					{
						text: '相手のクリーチャーを１体選ぶ。このターン、そのクリーチャーのパワーを－12000する。（パワー0以下のクリーチャーは破壊される）',
						position: 2
					},
					{
						text: 'このターン、相手のクリーチャーすべてのパワーを－3000する。',
						position: 3
					}
				]
			}
		]
	};

	const { Story } = defineMeta({
		component: AbilityText,
		argTypes: {
			effects: {
				control: { type: 'select' },
				options: ['icons', 'choice'],
				mapping: {
					icons: examples.icons,
					choice: examples.choice
				}
			}
		}
	});
</script>

<Story
	name="icons"
	args={{
		effects: [
			{
				position: 0,
				text: '[[icon:blocker]]ブロッカー',
				children: []
			},
			{
				position: 1,
				text: '[[icon:strigger]]S・トリガー（このカードを自分のシールドゾーンから手札に加える時、コストを支払わずにすぐ実行してもよい）'
			}
		]
	}}
/>

<Story
	name="choice"
	args={{
		effects: [
			{
				text: '次の中から１回選ぶ。',
				position: 1,
				children: [
					{
						text: '相手のクリーチャーを１体選ぶ。このターン、そのクリーチャーのパワーを－12000する。（パワー0以下のクリーチャーは破壊される）',
						position: 2
					},
					{
						text: 'このターン、相手のクリーチャーすべてのパワーを－3000する。',
						position: 3
					}
				]
			}
		]
	}}
/>
