# Minecraft Command Speech Manager
## Introduce
You can edit scoreboard score to manage your server's speech!

New command: `/sshout`

And it can also be controled by the scoreboard score!

## Examples
### Allow (Default)
```
/scoreboard players set test BAMBOO_MOD_SAYING 0
```

This will allow the members of team `test` speech.

### Only teammates
```
/scoreboard players set test BAMBOO_MOD_SAYING 2
```

### Only the members of other teams
```
/scoreboard players set test BAMBOO_MOD_SAYING 1
```
### Disallow all except command '/sshout'
```
/scoreboard players set test BAMBOO_MOD_SAYING 3
```

### Disallow all (Including command '/sshout')
```
/scoreboard players set test BAMBOO_MOD_SAYING 4
```