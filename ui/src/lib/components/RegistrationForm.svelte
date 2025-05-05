<script lang="ts">
	import { register, checkUsername } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';

	const { codeRequired } = $props();

	let username = $state('');
	let password = $state('');
	let code = $state('');
	let error = $state('');
	let usernameField: HTMLInputElement | undefined = $state();
	let usernameErrors = $state(false);
	let passwordField: HTMLInputElement | undefined = $state();
	let passwordErrors = $state(false);
	let codeField: HTMLInputElement | undefined = $state();
	let codeErrors = $state(false);

	async function handleSubmit() {
		validateUsername();
		validatePassword();
		validateCode();
		if (usernameErrors || passwordErrors || codeErrors) {
			return;
		}
		error = '';

		const authState = await register(fetch, { username, password, code });
		if (authState.authenticated) {
			await goto('/');
		} else {
			error = 'Registration failed';
		}
	}

	async function validatePassword() {
		passwordErrors = !passwordField?.checkValidity();
		if (passwordErrors) {
			passwordField?.setCustomValidity('');
			return;
		}
		const length = new TextEncoder().encode(password).length;
		if (length > 72) {
			passwordField?.setCustomValidity('Password is too long (72 characters maximum)');
			passwordErrors = true;
		} else if (length < 8) {
			passwordField?.setCustomValidity('Password is too short (8 characters minimum)');
			passwordErrors = true;
		} else {
			passwordField?.setCustomValidity('');
			passwordErrors = false;
		}
		passwordErrors = !passwordField?.checkValidity();
	}

	async function validateUsername() {
		usernameErrors = !usernameField?.checkValidity();
		if (usernameErrors) {
			usernameField?.setCustomValidity('');
			return;
		}
		const isAvailable = await checkUsername(fetch, username);
		if (isAvailable) {
			usernameField?.setCustomValidity('');
		} else {
			usernameField?.setCustomValidity('Username is already taken');
		}
		usernameErrors = !usernameField?.checkValidity();
	}

	async function validateCode() {
		if (!codeRequired) {
			return;
		}
		codeErrors = !codeField?.checkValidity();
	}
</script>

<form>
	<div class="flex flex-col gap-4">
		<h1 class="txt-h1 self-center">Create a new account</h1>
		<div class="mx-auto flex w-1/3 flex-col gap-2">
			<div class="flex flex-col">
				<label for="username">Username:</label>
				<span class="text-red-700" class:hidden={!usernameErrors}
					>{usernameField?.validationMessage}</span
				>
				<input
					class="select"
					class:invalid:border-red-700={usernameErrors}
					id="username"
					type="text"
					bind:this={usernameField}
					bind:value={username}
					onchange={validateUsername}
					oninput={() => (usernameErrors = false)}
					onkeyup={(e) => {
						if (e.key === 'Enter') {
							document.getElementById('password')?.focus();
						}
					}}
					required
					minlength="3"
					maxlength="50"
				/>
			</div>
			<div class="my-2">
				<div class="flex flex-col">
					<label for="password">Password:</label>
					<span class="text-red-700" class:hidden={!passwordErrors}
						>{passwordField?.validationMessage}</span
					>
					<input
						class="select"
						class:invalid:border-red-700={passwordErrors}
						id="password"
						type="password"
						autocomplete="new-password"
						bind:this={passwordField}
						bind:value={password}
						onchange={validatePassword}
						oninput={() => (passwordErrors = false)}
						onkeyup={(e) => {
							if (e.key === 'Enter') {
								if (codeRequired) {
									document.getElementById('code')?.focus();
								} else {
									handleSubmit();
								}
							}
						}}
						minlength="8"
						required
					/>
				</div>
			</div>
			{#if codeRequired}
				<div class="flex flex-col">
					<label for="code">Invitation code:</label>
					<span class="text-red-700" class:hidden={!codeErrors}>{codeField?.validationMessage}</span
					>
					<input
						class="select"
						id="code"
						type="text"
						bind:this={codeField}
						bind:value={code}
						onchange={validateCode}
						oninput={() => (codeErrors = false)}
						onkeyup={(e) => {
							if (e.key === 'Enter') {
								handleSubmit();
							}
						}}
						required
					/>
				</div>
			{/if}
		</div>

		{#if error}
			<div class="w-1/3 self-center text-lg text-red-700">{error}</div>
		{/if}

		<p class="w-1/3 self-center">
			There is currently no way to restore access to an account if the password is lost. Make sure
			to save it in a password manager.
		</p>

		<button class="btn-primary mx-auto w-1/3 justify-center" type="button" onclick={handleSubmit}
			>Create account</button
		>

		<p class="self-center">
			Already have an account? <a
				class="cursor-pointer text-inherit text-teal-700 underline visited:text-violet-700 visited:decoration-dashed"
				href="/login">Sign in here</a
			>
		</p>
	</div>
</form>
