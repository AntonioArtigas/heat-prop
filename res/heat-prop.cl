kernel void simulate(read_only image2d_t input, write_only image2d_t output,
                     read_only image2d_t alloy, int width, int height) {
  int2 coords = (int2)(get_global_id(0), get_global_id(1));

  write_imagef(output, (int2)(1, 1), 1000.0f);
  write_imagef(output, (int2)(width - 2, height - 2), 1000.0f);

  int2 north = (int2)(coords.x, coords.y - 1);
  int2 east = (int2)(coords.x + 1, coords.y);
  int2 south = (int2)(coords.x, coords.y + 1);
  int2 west = (int2)(coords.x - 1, coords.y);

  float4 northAlloy = read_imagef(alloy, north);
  float4 eastAlloy = read_imagef(alloy, east);
  float4 southAlloy = read_imagef(alloy, south);
  float4 westAlloy = read_imagef(alloy, west);

  float northTemp = read_imagef(input, north).r;
  float eastTemp = read_imagef(input, east).r;
  float southTemp = read_imagef(input, south).r;
  float westTemp = read_imagef(input, west).r;

  float3 thermalConstants = (float3)(0.75f, 1.0f, 1.25f);

  float temp = 0.0f;
  for (int i = 0; i < 3; i++) {
    float neighborTemp = 0.0f;

    int neighbors = 0;
    if (coords.y - 1 >= 0) {
      neighbors++;
      neighborTemp += northTemp * 1.0f / 3.0f;//northAlloy[i];
    }
    if (coords.x + 1 < width) {
      neighbors++;
      neighborTemp += eastTemp * 1.0f / 3.0f;//eastAlloy[i];
    }
    if (coords.y + 1 < height) {
      neighbors++;
      neighborTemp += southTemp * 1.0f / 3.0f;//southAlloy[i];
    }
    if (coords.x - 1 >= 0) {
      neighbors++;
      neighborTemp += westTemp * 1.0f / 3.0f;//westAlloy[i];
    }

    temp += thermalConstants[i] * neighborTemp / neighbors;
  }

  write_imagef(output, coords, temp);
}