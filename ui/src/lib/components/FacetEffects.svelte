<script lang="ts">
	import type { CardEffect } from '$lib/types/card';

	let { effects }: { effects: Array<CardEffect>} = $props();
</script>
<style>
    ol li ol li::marker{
        content: "▶ ";
    }
    ol li::marker {
        content: "■ "
    }
</style>
<div>
<ol class="list-inside list-disc">
{#each effects as effect (effect.position)}
 <li class="text-base">{effect.text}
    {#if effect.children}
    <ol class="list-inside pl-8">
    {#each effect.children as child (child.position)}
     <li class="text-base">{child.text}</li>   
    {/each}
    </ol>
    {/if}
 </li>   
{/each}
</ol></div>