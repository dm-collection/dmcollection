<script lang="ts">
	import PlusIcon from 'phosphor-svelte/lib/PlusIcon';
	import MinusIcon from 'phosphor-svelte/lib/MinusIcon';

	type Props = {
		value: number;
		min?: number;
		max?: number;
		showMax?: boolean;
		enforceMax?: boolean;
		onChange: (value: number) => void;
	};

	const MAX_INT = 2 ** 31 - 1;

	const {
		value,
		min = -(2 ** 31),
		max = MAX_INT,
		showMax = false,
		enforceMax = true,
		onChange
	}: Props = $props();

	async function increment() {
		if (value > MAX_INT || (enforceMax && value > max)) {
			return;
		}
		onChange(value + 1);
	}

	async function decrement() {
		if (value > min) {
			onChange(value - 1);
		}
	}
</script>

<form class="mx-auto">
	<div class="flex flex-row items-center justify-center">
		<button
			onclick={decrement}
			disabled={value <= min}
			aria-label="decrease"
			type="button"
			id="decrement-button"
			data-input-counter-decrement="quantity-input"
			class="btn-secondary p-1.5 md:p-2"
		>
			<MinusIcon weight="bold"><title>Subtract</title></MinusIcon>
		</button>
		<input
			id="quantity-input"
			data-input-counter
			class={['mx-2 w-[4ch] text-center text-lg text-gray-900', value > max && 'text-red-500']}
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
			class="btn-secondary p-1.5 md:p-2"
		>
			<PlusIcon weight="bold"><title>Add</title></PlusIcon>
		</button>
	</div>
</form>
