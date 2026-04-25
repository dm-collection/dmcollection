<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import AbilityTextLine from '$lib/components/card/AbilityTextLine.svelte';

	const { Story } = defineMeta({
		component: AbilityTextLine,
		argTypes: {
			text: { control: 'text' }
		}
	});
</script>

<Story
	name="Shield Trigger"
	args={{
		text: '[[icon:strigger]]S・トリガー（このカードを自分のシールドゾーンから手札に加える時、コストを支払わずにすぐ実行してもよい）'
	}}
/>

<Story
	name="Guard Strike"
	args={{
		text: '[[icon:gstrike]]G・ストライク（このカードを自分のシールドゾーンから手札に加える時、表向きにし、相手のクリーチャーを１体選んでもよい。このターン、そのクリーチャーは攻撃できない）'
	}}
/>

<Story
	name="Blocker"
	args={{
		text: '[[icon:blocker]]ブロッカー（このクリーチャーをタップして、相手クリーチャーの攻撃先をこのクリーチャーに変更してもよい）'
	}}
/>

<Story
	name="Double Breaker"
	args={{
		text: 'W・ブレイカー（このクリーチャーはシールドを２つブレイクする）'
	}}
/>

<Story
	name="D-d-dangeroudash"
	args={{
		text: 'D・D・D[[mana:lfn04]]（自分のクリーチャーが攻撃する時、このカードを[[mana:lfn04]]支払って自分の手札から実行してもよい）'
	}}
/>
