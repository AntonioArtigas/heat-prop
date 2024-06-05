package assignment3.solutions;

import assignment3.Alloy;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLContextCallbackI;
import org.lwjgl.opencl.CLImageDesc;
import org.lwjgl.opencl.CLImageFormat;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opencl.CL30.*;

public class GpuSolution extends SwingSolution {
    private static String deviceTypeString(long deviceType) {
        return switch ((int) deviceType) {
            case CL_DEVICE_TYPE_DEFAULT -> "DEFAULT";
            case CL_DEVICE_TYPE_CPU -> "CPU";
            case CL_DEVICE_TYPE_GPU -> "GPU";
            default -> "UNKNOWN";
        };
    }

    private static String getPlatformInfo(long platform, int param) {
        try (var stack = MemoryStack.stackPush()) {
            var pp = stack.mallocPointer(1);
            clGetPlatformInfo(platform, param, (ByteBuffer) null, pp);

            int numBytes = (int) pp.get(0);
            var buffer = stack.malloc(numBytes);
            clGetPlatformInfo(platform, param, buffer, null);

            return MemoryUtil.memUTF8(buffer, numBytes - 1);
        }
    }

    private static String getDeviceInfo(long device, int param) {
        try (var stack = MemoryStack.stackPush()) {
            var pp = stack.mallocPointer(1);
            clGetDeviceInfo(device, param, (ByteBuffer) null, pp);

            int numBytes = (int) pp.get(0);
            var buffer = stack.malloc(numBytes);
            clGetDeviceInfo(device, param, buffer, null);

            return MemoryUtil.memUTF8(buffer, numBytes - 1);
        }
    }

    private static long getDeviceLong(long device, int param) {
        try (var stack = MemoryStack.stackPush()) {
            var pl = stack.mallocLong(1);
            clGetDeviceInfo(device, param, pl, null);
            return pl.get(0);
        }
    }

    private static void printPlatformInfo(long platform, String paramName, int param) {
        System.out.println(paramName + ": " + getPlatformInfo(platform, param));
    }

    private static void printDeviceInfo(long device, String paramName, int param) {
        System.out.println(paramName + ": " + getDeviceInfo(device, param));
    }

    private static void printDeviceLong(long device, String paramName, int param) {
        System.out.println(paramName + ": " + getDeviceLong(device, param));
    }

    private final long platform;
    private final long device;

    private final long context;

    private final long kernel;

    private final long alloyImage;
    private final long workImage;
    private final long referenceImage;

    private long outputImage;
    private long inputImage;

    private final long commandQueue;


    private long createMeshImage() {
        var data = MemoryUtil.memAlignedAlloc(4096, width * height * Float.BYTES);
        MemoryUtil.memSet(data, 0);

        // https://man.opencl.org/clCreateImage.html
        try (var stack = MemoryStack.stackPush()) {
            var pErr = stack.mallocInt(1);
            CLImageFormat format = CLImageFormat.calloc(stack)
                    .image_channel_data_type(CL_UNSIGNED_INT32)
                    .image_channel_order(CL_R);

            CLImageDesc desc = CLImageDesc.calloc(stack)
                    .image_type(CL_MEM_OBJECT_IMAGE2D)
                    .image_width(width)
                    .image_height(height)
                    .image_array_size(1);

            // We want to be able to read and write to it AND have it exposed to the host.
            int flags = CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR;
            long image = clCreateImage(context, flags, format, desc, data, pErr);
            if (pErr.get(0) != CL_SUCCESS) {
                throw new IllegalStateException("Could not create image for mesh!");
            }

            MemoryUtil.memFree(data);

            return image;
        }
    }

    private long createAlloyImage() {
        // Using CL_MEM_COPY_HOST_PTR requires memory to be aligned a certain way AND be divisible by 64 bytes.
        var data = MemoryUtil.memAlignedAlloc(4096, width * height * 4 * Float.BYTES);
        for (int i = 0; i < width * height; i++) {
            var alloy = randomAlloy();
            data.putFloat(alloy.percent(0));
            data.putFloat(alloy.percent(1));
            data.putFloat(alloy.percent(2));
            data.putFloat(0f);
        }
        data.flip();

        // https://man.opencl.org/clCreateImage.html
        try (var stack = MemoryStack.stackPush()) {
            var pErr = stack.mallocInt(1);
            CLImageFormat format = CLImageFormat.calloc(stack)
                    .image_channel_data_type(CL_UNSIGNED_INT32)
                    .image_channel_order(CL_RGBA);

            CLImageDesc desc = CLImageDesc.calloc(stack)
                    .image_type(CL_MEM_OBJECT_IMAGE2D)
                    .image_width(width)
                    .image_height(height)
                    .image_array_size(1);

            // We want to be able to read and write to it. HOST_PTR is to tell OpenCL to copy the memory provided.
            int flags = CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR;
            long image = clCreateImage(context, flags, format, desc, data, pErr);
            if (pErr.get(0) != CL_SUCCESS) {
                throw new IllegalStateException("Could not create image for alloy!");
            }

            MemoryUtil.memFree(data);

            return image;
        }
    }

    private long loadKernel() {
        try (var stack = MemoryStack.stackPush()) {
            var pErr = stack.mallocInt(1);
            var source = Files.readString(Path.of("res/heat-prop.cl"));
            long program = clCreateProgramWithSource(context, source, pErr);

            if (pErr.get(0) != CL_SUCCESS) {
                System.out.println("Could not create program!");
            }

            int result = clBuildProgram(program, device, "", null, MemoryUtil.NULL);
            if (result != CL_SUCCESS) {
                System.err.println("Could not build program!");
            }

            long kernel = clCreateKernel(program, "simulate", pErr);
            if (pErr.get(0) != CL_SUCCESS) {
                System.out.println("Could not create kernel! code: " + pErr.get(0));
            }
            return kernel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long findPlatform() {
        try (var stack = MemoryStack.stackPush()) {
            var pNumPlatforms = stack.mallocInt(1);
            clGetPlatformIDs(null, pNumPlatforms); // Query number of platforms.

            int numPlatforms = pNumPlatforms.get(0);
            if (numPlatforms == 0) {
                throw new IllegalStateException("Couldn't find a platform.");
            }

            var pPlatforms = stack.mallocPointer(numPlatforms);
            clGetPlatformIDs(pPlatforms, (IntBuffer) null); // Get platforms through pointer.

            long platform = pPlatforms.get(0);

            System.out.println("Platform found!");
            printPlatformInfo(platform, "CL_PLATFORM_PROFILE", CL_PLATFORM_PROFILE);
            printPlatformInfo(platform, "CL_PLATFORM_VERSION", CL_PLATFORM_VERSION);
            printPlatformInfo(platform, "CL_PLATFORM_NAME", CL_PLATFORM_NAME);
            printPlatformInfo(platform, "CL_PLATFORM_VENDOR", CL_PLATFORM_VENDOR);
            printPlatformInfo(platform, "CL_PLATFORM_EXTENSIONS", CL_PLATFORM_EXTENSIONS);

            // We're going to use the first platform.
            return platform;
        }
    }

    private long getDevice() {
        try (var stack = MemoryStack.stackPush()) {
            var pNumPlatforms = stack.mallocInt(1);
            clGetPlatformIDs(null, pNumPlatforms);

            var devices = stack.mallocPointer(pNumPlatforms.get(0));
            clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, devices, (IntBuffer) null);

            System.out.println();
            System.out.println("Picking first device...");

            long device = devices.get(0);
            long deviceType = getDeviceLong(device, CL_DEVICE_TYPE);
            System.out.println("\tCL_DEVICE_TYPE: " + deviceTypeString(deviceType));

            return device;
        }
    }

    private long createContext() {
        try (var stack = MemoryStack.stackPush()) {
            var contextProps = stack.mallocPointer(3)
                    .put(0, CL_CONTEXT_PLATFORM)
                    .put(1, platform)
                    .put(2, 0);

            System.out.println();
            CLContextCallbackI contextCallback = (errInfo, privateInfo, cb, userData) -> System.out.printf("cl_context_callback\n\tInfo: %s%n", MemoryUtil.memUTF8(errInfo));
            System.out.println("Creating context...");
            return clCreateContext(contextProps, device, contextCallback, MemoryUtil.NULL, null);
        }
    }

    private long createCommandQueue() {
        return clCreateCommandQueue(context, device, MemoryUtil.NULL, (IntBuffer) null);
    }

    public GpuSolution(int width, int height, int maxIterations, float s, float t, float[] thermalConstants, int threshold) {
        // Threshold isn't used by the Gpu solution.
        super(width, height, maxIterations, s, t, thermalConstants, threshold);

        // Initialize OpenCL.
        platform = findPlatform();
        CL.createPlatformCapabilities(platform);

        device = getDevice();
        context = createContext();

        System.out.println("Loading kernel...");
        kernel = loadKernel();

        System.out.println("Creating images...");
        workImage = createMeshImage();
        referenceImage = createMeshImage();

        outputImage = workImage;
        inputImage = referenceImage;

        alloyImage = createAlloyImage();

        System.out.println("Creating queue...");
        commandQueue = createCommandQueue();
    }

    private void setKernelArgs() {
        try (var stack = MemoryStack.stackPush()) {
            var pArg = stack.mallocLong(1);
            pArg.put(0, inputImage);
            clSetKernelArg(kernel, 0, pArg);

            pArg.put(0, outputImage);
            clSetKernelArg(kernel, 1, pArg);

            pArg.put(0, alloyImage);
            clSetKernelArg(kernel, 2, pArg);

            clSetKernelArg1i(kernel, 3, width);
            clSetKernelArg1i(kernel, 4, height);
        }
    }

    @Override
    protected void compute() {
        setKernelArgs();
        try (var stack = MemoryStack.stackPush()) {
            var pWorkSize = stack.mallocPointer(2)
                    .put(0, width)
                    .put(1, height);

            // Call into kernel...
            int success = clEnqueueNDRangeKernel(commandQueue, kernel, 2, null, pWorkSize, null, null, null);
            if (success != CL_SUCCESS) {
                System.out.println("enqueue failed! code: " + success);
            }

            // And block until it finishes!
            success = clFinish(commandQueue);
            if (success != CL_SUCCESS) {
                System.out.println("clFinish failed!");
            }
        }
    }

    @Override
    public void run() {
        System.out.println("Raring to go!");

        for (int i = 0; i < maxIterations; i++) {
            compute();

            try (var stack = MemoryStack.stackPush()) {
                var pOrigin = stack.mallocPointer(3)
                        .put(0, 0)
                        .put(1, 0)
                        .put(2, 0);

                var pRegion = stack.mallocPointer(3)
                        .put(0, width)
                        .put(1, height)
                        .put(2, 1);

                var pImageRowPitch = stack.mallocPointer(1); // In bytes! Not floats!
                var pImageSlicePitch = stack.mallocPointer(1);

                var pErr = stack.mallocInt(1);

                var byteBuffer = clEnqueueMapImage(
                        commandQueue,
                        outputImage,
                        true,
                        CL_MAP_READ,
                        pOrigin, pRegion,
                        pImageRowPitch, pImageSlicePitch,
                        null,
                        null,
                        pErr,
                        null
                );

                if (byteBuffer == null) {
                    throw new IllegalStateException("buffer is null!");
                }

                // Block until it's mapped.
//                clFinish(commandQueue);

                var floatBuffer = byteBuffer.asFloatBuffer();
                displayBuffer(floatBuffer);

                if (pErr.get(0) != CL_SUCCESS) {
                    System.out.println("map image failed! code: " + pErr.get(0));
                }

                int result = clEnqueueUnmapMemObject(commandQueue, outputImage, byteBuffer, null, null);
                if (result != CL_SUCCESS) {
                    System.out.println("Could not unmap image!");
                }
            }

            // Swap images
            long temp = outputImage;
            outputImage = inputImage;
            inputImage = temp;
        }
    }
}
