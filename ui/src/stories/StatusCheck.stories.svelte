<script module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import StatusCheck from '$lib/components/StatusCheck.svelte';

	const { Story } = defineMeta({
		component: StatusCheck,
		argTypes: {
			promise: {
				control: 'select',
				options: ['pending', 'success', 'failure', 'error'],
				mapping: {
					pending: new Promise(() => {}),
					success: Promise.resolve(true),
					failure: Promise.resolve(false),
					error: Promise.reject(new Error('Something went wrong'))
				}
			}
		}
	});
</script>

<Story
	name="Status Check"
	args={{
		promise: 'success',
		successMessage: 'Success!',
		pendingMessage: 'Loading…',
		failureMessage: 'Failed.',
		errorMessage: 'An error occurred.'
	}}
/>
