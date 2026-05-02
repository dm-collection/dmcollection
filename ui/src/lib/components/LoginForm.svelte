<script lang="ts">
	import { auth } from '$lib/auth.svelte';

	const { redirectPath = '/' }: { redirectPath?: string } = $props();

	let username = $state('');
	let password = $state('');
	let rememberMe = $state(true);
	let error = $state('');
	let loginForm: HTMLFormElement | undefined = $state();

	async function handleSubmit() {
		loginForm?.reportValidity();
		if (loginForm?.checkValidity()) {
			error = '';

			await auth.login({ username, password, rememberMe }, redirectPath);
			if (!auth.isAuthenticated) {
				error = 'Invalid username or password';
			}
		}
	}
</script>

<form bind:this={loginForm}>
	<div class="flex flex-col gap-4">
		<h1 class="txt-h1 self-center">Sign in to proceed</h1>
		<div class="mx-auto flex flex-col gap-2 md:w-1/3">
			<div class="flex flex-col justify-start">
				<label for="username">Username:</label>
				<input
					class="select peer user-invalid:border-red-700"
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
					class="select peer user-invalid:border-red-700"
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
			<label>
				<input type="checkbox" bind:checked={rememberMe} />
				Remember me for 30 days
			</label>
		</div>

		{#if error}
			<div class="mx-auto text-center text-lg text-red-700 md:w-1/3">{error}</div>
		{/if}

		<button class="btn-primary mx-auto justify-center md:w-1/3" type="button" onclick={handleSubmit}
			>Sign in</button
		>

		<p class="self-center">
			Want to create an account? <a
				class="cursor-pointer text-inherit text-teal-700 underline visited:text-violet-700 visited:decoration-dashed"
				href="/register">Register here</a
			>
		</p>
	</div>
</form>
