# UI
This is a web UI using the Svelte framework. As it does not use sveltekit as a backend, it is configured with `@sveltejs/adapter-static`.
Running `npm run build` builds the project into `/build`. Copy the contents to `../server/src/main/resources/static/` and they will be served by the Spring-Boot server.
You can do both at once with `npm run deploy`.

For development, you can start the backend and then use `npm run dev` to serve the UI while changing the code.