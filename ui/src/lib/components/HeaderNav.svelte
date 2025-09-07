<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { logout } from '$lib/auth.svelte';

	type Props = {
		enabled?: boolean;
		username?: string;
	};

	const { enabled = true, username = '' }: Props = $props();

	function isActive(...prefixes: Array<string>): boolean {
		let result = false;
		const path = page.url.pathname;
		if (prefixes.length == 1 && prefixes[0] == '/') {
			return path == prefixes[0];
		}
		prefixes.forEach((prefix) => {
			result = result || path.startsWith(prefix);
		});
		return result;
	}

	async function handleLogout() {
		await logout(fetch);
	}
</script>

<header
	class="header sticky top-0 z-20 flex items-center justify-between bg-white px-8 py-0 shadow-md"
>
	<nav class="nav text-lg">
		<div class="flex">
			<a
				href={resolve('/')}
				class:pointer-events-none={!enabled}
				class:text-gray-600={!enabled}
				class:nav-links={enabled}
				class="p-3 {isActive('/') ? 'border-b-2 border-teal-700 font-bold text-teal-700' : ''}"
				>Home</a
			>
			<a
				href={resolve('/cards')}
				class:pointer-events-none={!enabled}
				class:text-gray-600={!enabled}
				class:nav-links={enabled}
				class="p-3 {isActive('/cards', '/card')
					? 'border-b-2 border-teal-700 font-bold text-teal-700'
					: ''}"
			>
				Cards
			</a>
			<a
				href={resolve('/collection')}
				class:pointer-events-none={!enabled}
				class:text-gray-600={!enabled}
				class:nav-links={enabled}
				class="p-3 {isActive('/collection')
					? 'border-b-2 border-teal-700 font-bold text-teal-700'
					: ''}"
			>
				Collection
			</a>
			<a
				href={resolve('/decks')}
				class:pointer-events-none={!enabled}
				class:text-gray-600={!enabled}
				class:nav-links={enabled}
				class="p-3 {isActive('/decks', '/deck')
					? 'border-b-2 border-teal-700 font-bold text-teal-700'
					: ''}"
			>
				Decks
			</a>
		</div>
	</nav>
	{#if enabled}
		<div class="float-right space-x-4">
			<span class="text-sm text-gray-600">
				Logged in as {username}
			</span>
			<button onclick={handleLogout} class="btn-primary text-sm" type="button">Logout</button>
		</div>
	{/if}
</header>
