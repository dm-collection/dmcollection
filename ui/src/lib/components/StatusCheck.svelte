<script lang="ts">
	import CircleNotchIcon from 'phosphor-svelte/lib/CircleNotchIcon';
	import CheckCircleIcon from 'phosphor-svelte/lib/CheckCircleIcon';
	import XCircleIcon from 'phosphor-svelte/lib/XCircleIcon';

	let {
		promise,
		successMessage,
		pendingMessage,
		failureMessage,
		errorMessage
	}: {
		promise: Promise<unknown>;
		successMessage: string;
		pendingMessage: string;
		failureMessage: string;
		errorMessage: string;
	} = $props();
</script>

{#await promise}
	<span class="inline-flex items-center gap-1">
		<CircleNotchIcon class="animate-spin text-teal-700"></CircleNotchIcon>
		<span>{pendingMessage}</span>
	</span>
{:then result}
	{#if result}
		<span class="inline-flex items-center gap-1 text-green-700">
			<CheckCircleIcon />
			<span>{successMessage}</span>
		</span>
	{:else}
		<span class="inline-flex items-center gap-1 text-red-700">
			<XCircleIcon />
			<span>{failureMessage}</span>
		</span>
	{/if}
{:catch}
	<span class="inline-flex items-center gap-1 text-red-700">
		<XCircleIcon />
		<span>{errorMessage}</span>
	</span>
{/await}
