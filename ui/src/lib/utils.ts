export const debounce = <T extends (...args: unknown[]) => void>(
	func: T,
	wait: number,
	immediate = false
) => {
	let timeout: ReturnType<typeof setTimeout> | undefined;
	return function (this: unknown, ...args: Parameters<T>) {
		clearTimeout(timeout);
		if (immediate && !timeout) func.apply(this, args);
		timeout = setTimeout(() => {
			timeout = undefined;
			if (!immediate) func.apply(this, args);
		}, wait);
	};
};
