<script lang="ts">
	import Plus from 'phosphor-svelte/lib/Plus';
	import Minus from 'phosphor-svelte/lib/Minus';

	type Props = {
		value: number;
		min?: number;
		max?: number;
		showMax?: boolean;
		enforceMax?: boolean;
		onChange: (value: number) => void;
	};

	const {
		value,
		min = -(2 ** 31),
		max = 2 ** 31 - 1,
		showMax = false,
		enforceMax = true,
		onChange
	}: Props = $props();

	async function increment() {
		if (!enforceMax || value < max) {
			onChange(value + 1);
		}
	}

	async function decrement() {
		if (value > min) {
			onChange(value - 1);
		}
	}
</script>

<form class="mx-auto max-w-xs">
	<div class="relative flex max-w-32 items-center">
		<button
			onclick={decrement}
			disabled={value <= min}
			aria-label="decrease"
			type="button"
			id="decrement-button"
			data-input-counter-decrement="quantity-input"
			class="btn-secondary px-2 py-2"
		>
			<Minus weight="bold"><title>Subtract</title></Minus>
		</button>
		<input
			id="quantity-input"
			data-input-counter
			class={[
				'block h-11 w-full border-x-0 py-2.5 text-center text-lg text-gray-900',
				value > max && 'text-red-500'
			]}
			placeholder="0"
			value={showMax ? `${value}/${max}` : value}
			required
			readonly
			disabled
		/>
		<button
			disabled={enforceMax && value >= max}
			onclick={increment}
			aria-label="increase"
			type="button"
			id="increment-button"
			data-input-counter-increment="quantity-input"
			class="btn-secondary px-2 py-2"
		>
			<Plus weight="bold"><title>Add</title></Plus>
		</button>
	</div>
</form>
