<script lang="ts">
	let examples = [
		{ type: 'basic', id: 'dm01-001' },
		{ type: 'zero civ', id: 'promoy13-006' },
		{ type: 'portrait & multicolor', id: 'dm24ex2-040' },
		{ type: 'hypermode', id: 'dm24ex1-025' },
		{ type: 'twinpact', id: 'dm24ex2-Cho047' },
		{ type: 'zero twinpact', id: 'promoy17-sp4d02' },
		{ type: 'two-sided', id: 'dmr01-v01' },
		{ type: '3d card', id: 'dmx18-021' },
		{ type: 'No cost', id: 'dmex18-019' },
		{ type: 'longest name & highest cost', id: 'dm22ex1-Cho22' },
		{ type: 'quotes in name', id: 'dmrp11-m03' },
		{ type: 'times in name', id: 'dmr08-006' }
	];
</script>

<div class="flex items-center">
	<div class="mx-auto text-center">
		<h1
			class="bg-gradient-to-r from-teal-300 via-blue-400 to-purple-600 bg-clip-text text-3xl font-extrabold text-transparent sm:text-5xl"
		>
			Welcome to DM Collection
		</h1>

		<p class="mx-auto mt-4 max-w-xl sm:text-xl/relaxed">
			The place to track your Duel Masters Card Collection
		</p>
		<h2 class="my-2 block text-lg">Here are some example cards:</h2>

		<div class="shadow-xs relative flex w-full min-w-0 flex-col rounded bg-white">
			<div class="mb-0 rounded-t border-0 px-4 py-3">
				<div class="flex flex-wrap items-center">
					<h3 class="text-blueGray-700 text-base font-semibold">Examples</h3>
				</div>
			</div>
			<div class="block w-full overflow-x-auto">
				<table class="w-full border-collapse items-center bg-transparent">
					<thead>
						<tr>
							<th class="tbl-header">Description</th>
							<th class="tbl-header">Link</th>
						</tr></thead
					>
					<tbody>
						{#each examples as card}
							<tr class=" text-left hover:bg-teal-50 active:bg-teal-700 active:text-teal-50">
								<td class="tbl-row">{card.type}</td>
								<td class="tbl-row">
									<a class="hover:underline" href="/card/{card.id}">{card.id}</a>
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
		</div>
	</div>
</div>
