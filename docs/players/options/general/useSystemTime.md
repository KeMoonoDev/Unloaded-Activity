!!! note "Default value: **false**"

Determines if Unloaded Activity should use the system time or game time for
simulations. If this option is set to **true**, Unloaded Activity will now also
simulate blocks if the game is paused for a while, or if the server is closed
for a while.

A side effect of using this option is that sleeping to skip the night will not
work. This is because Unloaded Activity will not see a big jump between the
current time and the last time something was simulated.
