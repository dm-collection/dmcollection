<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';

	if (page.error?.message === 'unauthorized' || page.error?.message === 'forbidden') {
		goto('/login');
	}
</script>

<h1>{page.error?.message}</h1>
