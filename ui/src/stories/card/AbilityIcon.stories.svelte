<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import AbilityIcon from '$lib/components/card/AbilityIcon.svelte';

	const keywords = [
		'blocker',
		'breakbonus',
		'dswitch',
		'gstrike',
		'henrei',
		'herocard',
		'jingi',
		'nochoice',
		'strigger',
		'striggerpluseffect',
		'sstriggereffect',
		'strikeback',
		'survivor',
		'tap',
		'wavestrike',
		'unknown'
	];

	const { Story } = defineMeta({
		component: AbilityIcon,
		argTypes: {
			keyword: {
				control: { type: 'select' },
				options: keywords
			}
		},
		parameters: {
			layout: 'centered'
		}
	});
</script>

<Story name="Blocker" args={{ keyword: 'blocker' }} />
<Story name="Shield Trigger" args={{ keyword: 'strigger' }} />
<Story name="Shield Trigger Plus Effect" args={{ keyword: 'striggerpluseffect' }} />
<Story name="Super Shield Trigger Effect" args={{ keyword: 'sstriggereffect' }} />
<Story name="Tap" args={{ keyword: 'tap' }} />
<Story name="Survivor" args={{ keyword: 'survivor' }} />
<Story name="Strikeback" args={{ keyword: 'strikeback' }} />
<Story name="Guard Strike" args={{ keyword: 'gstrike' }} />
<Story name="Breakbonus" args={{ keyword: 'breakbonus' }} />
<Story name="D-Switch" args={{ keyword: 'dswitch' }} />
<Story name="Henrei" args={{ keyword: 'henrei' }} />
<Story name="Hero Card" args={{ keyword: 'herocard' }} />
<Story name="Jingi" args={{ keyword: 'jingi' }} />
<Story name="No Choice" args={{ keyword: 'nochoice' }} />
<Story name="Wavestriker" args={{ keyword: 'wavestrike' }} />
<Story name="Fallback (unknown)" args={{ keyword: 'unknown' }} />
