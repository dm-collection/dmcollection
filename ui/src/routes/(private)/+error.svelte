<script lang="ts">
	import { page } from '$app/state';
	import { invalidateAuth } from '$lib/auth.svelte';

	if (page.error?.message === 'unauthorized' || page.error?.message === 'forbidden') {
		invalidateAuth();
	}
</script>

<h1>{page.error?.message}</h1>
