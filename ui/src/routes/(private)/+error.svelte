<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { invalidateAuth } from '$lib/auth.svelte';

	if (page.error?.message === 'unauthorized' || page.error?.message === 'forbidden') {
		invalidateAuth();
		goto('/login');
	}
</script>

<h1>{page.error?.message}</h1>
