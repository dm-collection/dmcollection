<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import { page } from '$app/state';
	import { logout } from '$lib/auth.svelte';
	import UserCircle from 'phosphor-svelte/lib/UserCircle';

	type Props = {
		enabled?: boolean;
		username?: string | null;
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

<NavigationMenu.Root class="sticky top-0 z-20 bg-white py-3 pl-2 text-lg shadow-md md:pl-8">
	<NavigationMenu.List class={['flex items-center']}>
		<NavigationMenu.Item>
			<NavigationMenu.Link
				href="/"
				class={[
					!enabled && 'pointer-events-none text-gray-600',
					enabled && 'nav-links',
					'py-3',
					'px-2',
					'sm:px-3',
					isActive('/') && 'border-b-2 border-teal-700 font-bold text-teal-700'
				]}
			>
				<span class="inline">Home</span>
			</NavigationMenu.Link>
		</NavigationMenu.Item>
		<NavigationMenu.Item>
			<NavigationMenu.Link
				href="/cards"
				class={[
					!enabled && 'pointer-events-none text-gray-600',
					enabled && 'nav-links',
					'py-3',
					'px-2',
					'sm:px-3',
					isActive('/card') && 'border-b-2 border-teal-700 font-bold text-teal-700'
				]}
			>
				<span class="inline">Cards</span>
			</NavigationMenu.Link>
		</NavigationMenu.Item>
		<NavigationMenu.Item>
			<NavigationMenu.Link
				href="/collection"
				class={[
					!enabled && 'pointer-events-none text-gray-600',
					enabled && 'nav-links',
					'py-3',
					'px-2',
					'sm:px-3',
					isActive('/collection') && 'border-b-2 border-teal-700 font-bold text-teal-700'
				]}
			>
				<span>Collection</span>
			</NavigationMenu.Link>
		</NavigationMenu.Item>
		<NavigationMenu.Item>
			<NavigationMenu.Link
				href="/decks"
				class={[
					!enabled && 'pointer-events-none text-gray-600',
					enabled && 'nav-links',
					'py-3',
					'px-2',
					'sm:px-3',
					isActive('/decks') && 'border-b-2 border-teal-700 font-bold text-teal-700'
				]}
			>
				<span>Decks</span>
			</NavigationMenu.Link>
		</NavigationMenu.Item>
		{#if enabled && username}
			<NavigationMenu.Item class="relative ml-auto pr-2 md:pr-8">
				<NavigationMenu.Trigger>
					<span
						><UserCircle class="inline" size="1.7em" weight="duotone" color="var(--color-teal-700)"
						></UserCircle><span class="hidden align-middle text-teal-700 sm:ml-2 sm:inline"
							>{username}</span
						></span
					>
				</NavigationMenu.Trigger>
				<NavigationMenu.Content class="absolute right-0 mt-3 rounded-b-sm bg-white p-3 shadow-sm">
					<div class="flex flex-col items-center gap-3">
						<p class="text-teal-700 sm:hidden">{username}</p>
						<button onclick={handleLogout} class="btn-primary text-sm" type="button">Logout</button>
					</div>
				</NavigationMenu.Content>
			</NavigationMenu.Item>
		{/if}
	</NavigationMenu.List>
</NavigationMenu.Root>
