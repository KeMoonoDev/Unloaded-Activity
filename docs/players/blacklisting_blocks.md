# Blacklisting blocks

Sometimes, Unloaded Activity will not properly simulate certain blocks.
Especially modded blocks. One thing you can do in the meantime while the bug is
getting fixed is to blacklist the blocks.

## Adding/Removing blocks to the blacklist

There are 2 ways to add blocks to the blacklist:

### Config file

The config file can be found over at `config/unloadedactivity.json` in your
Minecraft/server directory. Inside, there's a list assigned to
`blacklistedBlocks`, and you simply add your block or block tag to that list.

Example:

```json
{
    ...
    "blacklistedBlocks": [
        "minecraft:grass_block",
        "#minecraft:crops"
    ],
    ...
}
```

### In-game

To add a block or block tag to the blacklist in-game, you can use this command:
<br> **`/unloadedactivity config blacklistedBlocks add`**

To remove a block/tag, you use this command:
<br> **`/unloadedactivity config blacklistedBlocks remove`**

To list all currently blacklisted blocks, use this command:
<br> **`/unloadedactivity config blacklistedBlocks list`**
