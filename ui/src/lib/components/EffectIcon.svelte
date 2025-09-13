<script lang="ts">
	import Dswitch from './effectIcons/Dswitch.svelte';
	import Blocker from './effectIcons/Blocker.svelte';
	import Breakbonus from './effectIcons/Breakbonus.svelte';
	import Fallback from './effectIcons/Fallback.svelte';
	import Henrei from './effectIcons/Henrei.svelte';
	import Jingi from './effectIcons/Jingi.svelte';
	import NoChoice from './effectIcons/NoChoice.svelte';
	import ShieldTrigger from './effectIcons/ShieldTrigger.svelte';
	import Strikeback from './effectIcons/Strikeback.svelte';
	import Survivor from './effectIcons/Survivor.svelte';
	import Tap from './effectIcons/Tap.svelte';
	import HeroCard from './effectIcons/HeroCard.svelte';
	import Wavestriker from './effectIcons/Wavestriker.svelte';
	import ShieldTriggerPlusEffect from './effectIcons/ShieldTriggerPlusEffect.svelte';
	import SuperShieldTriggerEffect from './effectIcons/SuperShieldTriggerEffect.svelte';
	import GuardStrike from './effectIcons/GuardStrike.svelte';

	const { keyword }: { keyword: string } = $props();

	const iconMap = {
		accel: Fallback,
		blocker: Blocker,
		breakbonus: Breakbonus,
		bsoul: Fallback,
		buzzrenda: Fallback,
		dswitch: Dswitch,
		episode2: Fallback,
		episode3: Fallback,
		esoul: Fallback,
		fortenergy: Fallback,
		gstrike: GuardStrike,
		henrei: Henrei,
		herocard: HeroCard,
		holyfield: Fallback,
		hsoul: Fallback,
		jingi: Jingi,
		kizuna: Fallback,
		knightmagic: Fallback,
		ksoul: Fallback,
		manadrive: Fallback,
		martialtouch: Fallback,
		metamorph: Fallback,
		msoul: Fallback,
		nochoice: NoChoice,
		overdrive: Fallback, // reused for キッカー
		sforce: Fallback,
		shieldgo: Fallback,
		silentskill: Fallback,
		spacecharge: Fallback,
		strigger: ShieldTrigger,
		striggerplus: Fallback,
		striggerpluseffect: ShieldTriggerPlusEffect,
		sstrigger: Fallback,
		sstriggereffect: SuperShieldTriggerEffect,
		strikeback: Strikeback,
		survivor: Survivor,
		tap: Tap,
		thrilling: Fallback,
		turborush: Fallback,
		victory: Fallback,
		wavestrike: Wavestriker,
		wsoul: Fallback
	};
</script>

{#if Object.prototype.hasOwnProperty.call(iconMap, keyword)}
	{@const SvelteComponent = iconMap[keyword as keyof typeof iconMap]}
	<SvelteComponent></SvelteComponent>
{:else}
	<Fallback></Fallback>
{/if}
