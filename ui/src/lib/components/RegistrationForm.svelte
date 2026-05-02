<script lang="ts">
	import { isUserNameAvailable, register } from '$lib/api/register';
	import { debounce } from '$lib/utils';
	import { goto } from '$app/navigation';
	import StatusCheck from './StatusCheck.svelte';

	const { codeRequired = false }: { codeRequired: boolean } = $props();

	let username = $state('');
	let registrationForm: HTMLFormElement | undefined = $state();
	let usernameInput: HTMLInputElement | undefined = $state();
	let usernameAvailable = $derived.by(() => {
		if (username.length > 0 && usernameInput?.checkValidity()) {
			return isUserNameAvailable(username);
		}
	});
	let password = $state('');
	let code = $state('');
	let error = $state('');
	let usernameValidity: string | undefined = $state();
	let passwordValidity: string | undefined = $state();
	let codeValidity: string | undefined = $state();

	const updateUsername = debounce((value: unknown) => {
		username = value as string;
	}, 500);

	async function handleSubmit() {
		error = '';
		registrationForm?.reportValidity();
		if (registrationForm?.checkValidity()) {
			const authState = await register({ username, password, code });
			if (authState.username) {
				await goto('/');
			} else {
				error = 'Registration failed';
			}
		}
	}
</script>

<form bind:this={registrationForm}>
	<div class="flex flex-col gap-4">
		<h1 class="txt-h1 self-center">Create a new account</h1>
		<div class="mx-auto flex flex-col gap-2 md:w-1/3">
			<div class="flex flex-col">
				<label for="username">Username:</label>
				<input
					class="select peer user-invalid:border-red-700"
					id="username"
					type="text"
					bind:this={usernameInput}
					oninput={(e) => updateUsername(e.currentTarget.value)}
					onchange={(e) => (usernameValidity = e.currentTarget.validationMessage)}
					onkeyup={(e) => {
						if (e.key === 'Enter') {
							document.getElementById('password')?.focus();
						}
					}}
					required
					minlength="3"
					maxlength="50"
				/>
				{#if usernameAvailable !== undefined}
					<StatusCheck
						promise={usernameAvailable}
						successMessage="Available"
						pendingMessage="Checking"
						failureMessage="Not available"
						errorMessage="Error checking availability"
					/>
				{/if}
				<span class="hidden text-sm text-red-700 peer-user-invalid:block">{usernameValidity}</span>
			</div>
			<div class="my-2">
				<div class="flex flex-col">
					<label for="password">Password:</label>
					<input
						class="select peer user-invalid:border-red-700"
						id="password"
						type="password"
						autocomplete="new-password"
						bind:value={password}
						onchange={(e) => (passwordValidity = e.currentTarget.validationMessage)}
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
						maxlength="72"
						required
					/>
					<span class="hidden text-sm text-red-700 peer-user-invalid:block">{passwordValidity}</span
					>
				</div>
			</div>
			{#if codeRequired}
				<div class="flex flex-col">
					<label for="code">Invitation code:</label>
					<input
						class="select peer user-invalid:border-red-700"
						id="code"
						type="text"
						bind:value={code}
						onchange={(e) => (codeValidity = e.currentTarget.validationMessage)}
						onkeyup={(e) => {
							if (e.key === 'Enter') {
								handleSubmit();
							}
						}}
						required
					/>
					<span class="hidden text-sm text-red-700 peer-user-invalid:block">{codeValidity}</span>
				</div>
			{/if}
		</div>

		{#if error}
			<div class="self-center text-lg text-red-700 md:w-1/3">{error}</div>
		{/if}

		<div
			class="self-center rounded-md border-2 border-amber-200 bg-amber-100 p-2 text-center lg:w-1/3"
		>
			<p>There is currently no way to restore access to an account if the password is lost.</p>
			<p>Make sure to save it in a password manager.</p>
		</div>

		<button class="btn-primary mx-auto justify-center md:w-1/3" type="button" onclick={handleSubmit}
			>Create Account</button
		>

		<p class="self-center">
			Already have an account? <a
				class="cursor-pointer text-inherit text-teal-700 underline visited:text-violet-700 visited:decoration-dashed"
				href="/login">Sign in here</a
			>
		</p>
	</div>
</form>
