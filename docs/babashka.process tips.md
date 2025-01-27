1. When using `p/process` and `p/shell` a variable list of strings is expected at the end. When creating the command using a vector or similar, be sure to use `apply` so that the vector is unwrapped
	1. Example: `(apply p/process {} ["echo" "123"])`
2. Some useful flags for file processing scripts
	1. `--dry-run` only print actions, don’t execute
	2. `--verbose` log additional input