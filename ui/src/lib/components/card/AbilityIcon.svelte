<script lang="ts">
	import Dswitch from './abilityIcons/Dswitch.svelte';
	import Blocker from './abilityIcons/Blocker.svelte';
	import Breakbonus from './abilityIcons/Breakbonus.svelte';
	import Fallback from './abilityIcons/Fallback.svelte';
	import Henrei from './abilityIcons/Henrei.svelte';
	import Jingi from './abilityIcons/Jingi.svelte';
	import NoChoice from './abilityIcons/NoChoice.svelte';
	import ShieldTrigger from './abilityIcons/ShieldTrigger.svelte';
	import Strikeback from './abilityIcons/Strikeback.svelte';
	import Survivor from './abilityIcons/Survivor.svelte';
	import Tap from './abilityIcons/Tap.svelte';
	import HeroCard from './abilityIcons/HeroCard.svelte';
	import Wavestriker from './abilityIcons/Wavestriker.svelte';
	import ShieldTriggerPlusEffect from './abilityIcons/ShieldTriggerPlusEffect.svelte';
	import SuperShieldTriggerEffect from './abilityIcons/SuperShieldTriggerEffect.svelte';
	import GuardStrike from './abilityIcons/GuardStrike.svelte';

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
