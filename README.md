# heat-prop

A simple simulation of heat propagation through a sheet of metal alloy.

There are 3 versions of the simulation:
- Imperative: Simulates on a single core
- Parallel: Uses multiple threads to simulate
- Gpu: Hardware accelerated simulation via OpenCL

## Notes:
- Uses picolci to perform argument parsing
- Thanks to Victor Lockwood for helping me with some snags