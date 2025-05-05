<script lang="ts">
	import { login } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';

	let username = '';
	let password = '';
	let error = '';

	async function handleSubmit() {
		error = '';

		const authState = await login(fetch, { username, password });
		if (authState.authenticated) {
			const redirectPath = sessionStorage.getItem('redirectAfterLogin') || '/';
			sessionStorage.removeItem('redirectAfterLogin');
			await goto(redirectPath);
		} else {
			error = 'Invalid username or password';
		}
	}
</script>

<div class="flex flex-col gap-4">
	<h1 class="txt-h1 self-center">Sign in to proceed</h1>
	<div class="mx-auto flex w-1/3 flex-col gap-2">
		<div class="flex flex-col justify-start">
			<label for="username">Username:</label>
			<input
				class="select"
				id="username"
				type="text"
				autocomplete="username"
				bind:value={username}
				required
				onkeyup={(e) => {
					if (e.key === 'Enter') {
						document.getElementById('password')?.focus();
					}
				}}
			/>
		</div>

		<div class="flex flex-col justify-start">
			<label for="password">Password:</label>
			<input
				class="select"
				id="password"
				type="password"
				autocomplete="current-password"
				bind:value={password}
				required
				onkeyup={(e) => {
					if (e.key === 'Enter') {
						handleSubmit();
					}
				}}
			/>
		</div>
	</div>

	{#if error}
		<div class="text-lg text-red-700">{error}</div>
	{/if}

	<button class="btn-primary mx-auto w-1/3 justify-center" onclick={handleSubmit}>Sign in</button>

	<p class="self-center">
		Want to create an account? <a
			class="cursor-pointer text-inherit text-teal-700 underline visited:text-violet-700 visited:decoration-dashed"
			href="/register">Register here</a
		>
	</p>
</div>
