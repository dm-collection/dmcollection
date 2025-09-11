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

	const { keyword }: { keyword: string } = $props();

	const iconMap = {
		accel: Fallback,
		blocker: Blocker,
		strigger: ShieldTrigger,
		striggerplus: Fallback,
		striggerpluseffect: ShieldTriggerPlusEffect,
		strikeback: Strikeback,
		sstrigger: Fallback,
		sstriggereffect: SuperShieldTriggerEffect,
		sforce: Fallback,
		shieldgo: Fallback,
		victory: Fallback,
		tap: Tap,
		herocard: HeroCard,
		episode2: Fallback,
		episode3: Fallback,
		fortenergy: Fallback,
		thrilling: Fallback,
		knightmagic: Fallback,
		martialtouch: Fallback,
		metamorph: Fallback,
		holyfield: Fallback,
		spacecharge: Fallback,
		nochoice: NoChoice,
		msoul: Fallback,
		ksoul: Fallback,
		hsoul: Fallback,
		wsoul: Fallback,
		esoul: Fallback,
		bsoul: Fallback,
		henrei: Henrei,
		jingi: Jingi,
		breakbonus: Breakbonus,
		overdrive: Fallback, // reused for キッカー
		turborush: Fallback,
		wavestrike: Wavestriker,
		silentskill: Fallback,
		survivor: Survivor,
		kizuna: Fallback,
		gstrike: Fallback,
		buzzrenda: Fallback,
		manadrive: Fallback,
		dswitch: Dswitch
	};
</script>

{#if Object.prototype.hasOwnProperty.call(iconMap, keyword)}
	{@const SvelteComponent = iconMap[keyword as keyof typeof iconMap]}
	<SvelteComponent></SvelteComponent>
{:else}
	<Fallback></Fallback>
{/if}
