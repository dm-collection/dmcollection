<script lang="ts">
	import CircleNotch from 'phosphor-svelte/lib/CircleNotch';
	import CheckCircle from 'phosphor-svelte/lib/CheckCircle';
	import XCircle from 'phosphor-svelte/lib/XCircle';

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
		<CircleNotch class="animate-spin text-teal-700"></CircleNotch>
		<span>{pendingMessage}</span>
	</span>
{:then result}
	{#if result}
		<span class="inline-flex items-center gap-1 text-green-700">
			<CheckCircle />
			<span>{successMessage}</span>
		</span>
	{:else}
		<span class="inline-flex items-center gap-1 text-red-700">
			<XCircle />
			<span>{failureMessage}</span>
		</span>
	{/if}
{:catch}
	<span class="inline-flex items-center gap-1 text-red-700">
		<XCircle />
		<span>{errorMessage}</span>
	</span>
{/await}
